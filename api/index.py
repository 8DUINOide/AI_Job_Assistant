import os
import sys
import json
import time

# Add parent directory to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
from dotenv import load_dotenv
import PyPDF2
import csv
import io
import openpyxl

from tracker import log_application, get_recent_logs, log_applications_batch, update_application_status
from scraper import load_profile, scrape_jobs_multisite, evaluate_job, evaluate_jobs_batch
from emailer import send_job_digest

load_dotenv()

app = Flask(__name__)
CORS(app)

# -----------------
# 1. PUBLIC WEB APP
# -----------------
@app.route('/')
def home():
    """Serves the beautiful web app homepage."""
    # Vercel sets the CWD to the project root
    return send_file(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'index.html'))

@app.route('/api/profile', methods=['GET'])
def get_profile():
    try:
        profile = load_profile()
        return jsonify({"success": True, "profile": profile})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/logs', methods=['GET'])
def get_logs():
    try:
        logs = get_recent_logs(limit=20)
        if isinstance(logs, dict) and logs.get("auth_error"):
            return jsonify({"success": False, "auth_error": True, "error": "Google Sheets Authentication failed."}), 401
        return jsonify({"success": True, "logs": logs})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/run-agent-manually', methods=['POST'])
def run_agent_manually():
    """Fetches raw jobs based on master profile to bypass Vercel timeout."""
    try:
        profile = load_profile()
        roles = profile.get('job_preferences', {}).get('desired_roles', ['Software Engineer'])
        search_keyword = roles[0] if roles else "Software Engineer"
        
        # Scrape raw jobs from multiple platforms
        raw_jobs = scrape_jobs_multisite(search_keyword)
        
        return jsonify({
            "success": True, 
            "profile": profile,
            "jobs": raw_jobs
        })
    except Exception as e:
        print("Error running agent manually:", e)
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/evaluate-jobs', methods=['POST'])
def evaluate_multiple_jobs():
    """Evaluates multiple jobs in a single batch to bypass Gemini RPM limits and speed up execution."""
    data = request.json
    jobs = data.get('jobs', [])
    profile = data.get('profile')
    
    if not jobs or not profile:
        return jsonify({"success": False, "error": "Missing data"}), 400
        
    evaluated_results = evaluate_jobs_batch(jobs, profile)
    
    for i, job in enumerate(jobs):
        if i < len(evaluated_results):
            job['score'] = evaluated_results[i].get('score', 0)
            job['reason'] = evaluated_results[i].get('reason', 'Evaluation failed')
        else:
            job['score'] = 50
            job['reason'] = 'Evaluation parsing error'
            
    return jsonify({"success": True, "jobs": jobs})

@app.route('/api/evaluate-job', methods=['POST'])
def evaluate_single_job():
    """Evaluates a single job to bypass Vercel 10s limit."""
    data = request.json
    job = data.get('job')
    profile = data.get('profile')
    
    if not job or not profile:
        return jsonify({"success": False, "error": "Missing data"}), 400
        
    score, reason = evaluate_job(job['description'], profile)
    job['score'] = score
    job['reason'] = reason
    
    print(f"\nRole: {job['title']} @ {job['company']}")
    print(f"Link: {job['link']}")
    print(f"Match Score: {score}/100")
    print(f"Reason: {reason}")
    
    # Tiny delay to protect Gemini RPM limits (15/min)
    time.sleep(4)
    
    return jsonify({"success": True, "job": job})

@app.route('/api/send-digest', methods=['POST'])
def handle_send_digest():
    """Allows the public web app to email matches to a user's inputted email."""
    data = request.json
    email = data.get('email')
    jobs = data.get('jobs', [])
    
    if email and jobs:
        try:
            send_job_digest(email, jobs)
            
            # Log these jobs as pending
            import datetime
            today = datetime.datetime.now().strftime("%Y-%m-%d")
            rows = []
            for j in jobs:
                rows.append({
                    'company': j.get('company', 'Unknown'),
                    'job_title': j.get('title', 'Unknown'),
                    'tech_stack': '',
                    'status': 'Pending',
                    'date_applied': today,
                    'job_link': j.get('link', ''),
                    'location': j.get('location', ''),
                    'salary': j.get('salary', ''),
                    'contact_person': j.get('contact_person', '')
                })
            log_applications_batch(rows)
            
            return jsonify({"success": True})
        except Exception as e:
            return jsonify({"success": False, "error": str(e)}), 500
    return jsonify({"success": False, "error": "Missing email or jobs"}), 400
    
@app.route('/api/update-status', methods=['POST'])
def update_status():
    """Updates the status of a pending job."""
    data = request.json
    company = data.get('company')
    job_title = data.get('job_title')
    new_status = data.get('status')
    
    if company and job_title and new_status:
        try:
            success = update_application_status(company, job_title, new_status)
            if success:
                return jsonify({"success": True})
            else:
                return jsonify({"success": False, "error": "Could not find job to update."}), 404
        except Exception as e:
            return jsonify({"success": False, "error": str(e)}), 500
    return jsonify({"success": False, "error": "Missing data"}), 400

# -----------------
# 2. CHROME EXTENSION API
# -----------------
@app.route('/api/fill-form', methods=['POST'])
def fill_form():
    data = request.json
    fields = data.get('fields', [])
    profile = load_profile()
    
    answers = {}
    personal = profile.get('personal_info', {})
    
    for field in fields:
        field_id = field.get('id', '').lower()
        field_name = field.get('name', '').lower()
        label = field.get('label', '').lower()
        combined = f"{field_id} {field_name} {label}"
        
        target_key = field.get('id') or field.get('name')
        if not target_key:
            continue
            
        if 'first' in combined and 'name' in combined:
            answers[target_key] = personal.get('first_name', '')
        elif 'last' in combined and 'name' in combined:
            answers[target_key] = personal.get('last_name', '')
        elif 'email' in combined:
            answers[target_key] = personal.get('email', '')
        elif 'phone' in combined:
            answers[target_key] = personal.get('phone', '')
        elif 'location' in combined or 'city' in combined:
            answers[target_key] = personal.get('location', '')
        elif 'linkedin' in combined:
            answers[target_key] = personal.get('linkedin_url', '')
        elif 'portfolio' in combined or 'website' in combined:
            answers[target_key] = personal.get('portfolio_url', '')
            
    return jsonify({"answers": answers})

@app.route('/api/log-job', methods=['POST'])
def log_job():
    data = request.json
    url = data.get('url', '')
    title = data.get('title', 'Unknown Job')
    
    parts = title.split(' at ')
    job_name = parts[0].strip() if len(parts) > 1 else title
    company = parts[1].split('|')[0].strip() if len(parts) > 1 else "Unknown"
    
    import datetime
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    
    success = log_application(job_name, company, today, "Applied via Extension", url)
    if success:
        return jsonify({"success": True})
    return jsonify({"success": False}), 500

@app.route('/api/upload-logs', methods=['POST'])
def upload_logs():
    if 'file' not in request.files:
        return jsonify({"success": False, "error": "No file part"}), 400
        
    file = request.files['file']
    if file.filename == '':
        return jsonify({"success": False, "error": "No selected file"}), 400
        
    rows_to_insert = []
    
    try:
        if file.filename.endswith('.csv'):
            stream = io.StringIO(file.stream.read().decode("UTF8"), newline=None)
            csv_input = csv.reader(stream)
            headers = next(csv_input, None)
            for row in csv_input:
                if not row or not row[0].strip(): continue
                rows_to_insert.append({
                    'company': row[0] if len(row) > 0 else '',
                    'job_title': row[1] if len(row) > 1 else '',
                    'tech_stack': row[2] if len(row) > 2 else '',
                    'status': row[3] if len(row) > 3 else 'Applied',
                    'date_applied': row[4] if len(row) > 4 else '',
                    'job_link': row[5] if len(row) > 5 else ''
                })
        elif file.filename.endswith('.xlsx'):
            wb = openpyxl.load_workbook(file)
            ws = wb.active
            is_header = True
            for row in ws.iter_rows(values_only=True):
                if is_header:
                    is_header = False
                    continue
                if not row or not row[0]: continue
                
                rows_to_insert.append({
                    'company': str(row[0]) if len(row) > 0 and row[0] else '',
                    'job_title': str(row[1]) if len(row) > 1 and row[1] else '',
                    'tech_stack': str(row[2]) if len(row) > 2 and row[2] else '',
                    'status': str(row[3]) if len(row) > 3 and row[3] else 'Applied',
                    'date_applied': str(row[4]) if len(row) > 4 and row[4] else '',
                    'job_link': str(row[5]) if len(row) > 5 and row[5] else ''
                })
        else:
            return jsonify({"success": False, "error": "Invalid file type. Please upload .xlsx or .csv"}), 400
            
        success = log_applications_batch(rows_to_insert)
        if success:
            return jsonify({"success": True, "count": len(rows_to_insert)})
        else:
            return jsonify({"success": False, "error": "Failed to log to Google Sheets. Check your credentials.json!"}), 500
            
    except Exception as e:
        print("Upload Error:", e)
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/upload-credentials', methods=['POST'])
def upload_credentials():
    if 'file' not in request.files:
        return jsonify({"success": False, "error": "No file part"}), 400
        
    file = request.files['file']
    if file.filename == '':
        return jsonify({"success": False, "error": "No selected file"}), 400
        
    if not file.filename.endswith('.json'):
        return jsonify({"success": False, "error": "Must be a .json file"}), 400
        
    try:
        # Save the file to the project root
        file.save(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'credentials.json'))
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

# -----------------
# 3. BACKGROUND CRON
# -----------------
@app.route('/api/cron', methods=['GET', 'POST'])
def run_scraper_cron():
    """This endpoint is triggered daily by Vercel Cron."""
    profile = load_profile()
    roles = profile.get('job_preferences', {}).get('desired_roles', ['Software Engineer'])
    search_keyword = roles[0] if roles else "Software Engineer"
    
    found_jobs = scrape_jobs_multisite(search_keyword)
    high_match_jobs = []
    
    if found_jobs:
        evaluated_results = evaluate_jobs_batch(found_jobs, profile)
        for i, job in enumerate(found_jobs):
            if i < len(evaluated_results):
                score = evaluated_results[i].get('score', 0)
                if score > 70:
                    job['score'] = score
                    job['reason'] = evaluated_results[i].get('reason', '')
                    high_match_jobs.append(job)
            
    if high_match_jobs:
        send_job_digest(os.getenv("EMAIL_TARGET", "alfrancisbadillapaz10@gmail.com"), high_match_jobs)
        
        # Log these jobs as pending
        import datetime
        today = datetime.datetime.now().strftime("%Y-%m-%d")
        rows = []
        for j in high_match_jobs:
            rows.append({
                'company': j.get('company', 'Unknown'),
                'job_title': j.get('title', 'Unknown'),
                'tech_stack': '',
                'status': 'Pending',
                'date_applied': today,
                'job_link': j.get('link', ''),
                'location': j.get('location', ''),
                'salary': j.get('salary', ''),
                'contact_person': j.get('contact_person', '')
            })
        log_applications_batch(rows)
        
    return jsonify({"status": "completed", "jobs_found": len(high_match_jobs)})

application = app

if __name__ == '__main__':
    app.run(debug=True, port=5000)
