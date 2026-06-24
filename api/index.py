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

from tracker import log_application, get_recent_logs
from scraper import load_profile, scrape_linkedin_jobs, evaluate_job, evaluate_jobs_batch
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
        logs = get_recent_logs(limit=10)
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
        
        # Scrape raw jobs from LinkedIn
        raw_jobs = scrape_linkedin_jobs(search_keyword)
        
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
            return jsonify({"success": True})
        except Exception as e:
            return jsonify({"success": False, "error": str(e)}), 500
    return jsonify({"success": False, "error": "Missing email or jobs"}), 400

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

# -----------------
# 3. BACKGROUND CRON
# -----------------
@app.route('/api/cron', methods=['GET', 'POST'])
def run_scraper_cron():
    """This endpoint is triggered daily by Vercel Cron."""
    profile = load_profile()
    roles = profile.get('job_preferences', {}).get('desired_roles', ['Software Engineer'])
    search_keyword = roles[0] if roles else "Software Engineer"
    
    found_jobs = scrape_linkedin_jobs(search_keyword)
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
        
    return jsonify({"status": "completed", "jobs_found": len(high_match_jobs)})

application = app

if __name__ == '__main__':
    app.run(debug=True, port=5000)
