import os
import sys
import json
import time

# Add parent directory to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import google.generativeai as genai
from dotenv import load_dotenv
import PyPDF2

from tracker import log_application
from scraper import load_profile, scrape_linkedin_jobs, evaluate_job
from emailer import send_job_digest

load_dotenv()
os.environ.pop("GOOGLE_APPLICATION_CREDENTIALS", None) # Fix for Google Auth intercepting Gemini API keys
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
model = genai.GenerativeModel('gemini-1.5-flash')

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

@app.route('/api/upload-resume', methods=['POST'])
def upload_resume():
    """Parses PDF and fetches raw jobs to bypass Vercel timeout."""
    if 'resume' not in request.files:
        return jsonify({"success": False, "error": "No resume uploaded"}), 400
        
    file = request.files['resume']
    
    try:
        # Extract text from PDF in memory
        reader = PyPDF2.PdfReader(file)
        text = ""
        for page in reader.pages:
            text += page.extract_text() + "\n"
            
        # Ask Gemini to quickly extract the core role and skills
        prompt = f"""
        Analyze this resume text. Extract the candidate's core job title (e.g. "Frontend Developer", "Data Scientist") and a list of their top 5 skills.
        Return ONLY a JSON object like this:
        {{"job_title": "Backend Engineer", "skills": ["Python", "Flask", "SQL", "Git", "Docker"]}}
        
        Resume text:
        {text[:5000]}
        """
        
        try:
            response = model.generate_content(prompt)
            res_text = response.text.strip()
            if res_text.startswith("```json"): res_text = res_text[7:]
            if res_text.startswith("```"): res_text = res_text[3:]
            if res_text.endswith("```"): res_text = res_text[:-3]
            
            profile_data = json.loads(res_text)
        except Exception as api_err:
            print(f"Gemini API failed ({api_err}). Using Fallback Keyword Extractor...")
            text_lower = text.lower()
            common_skills = ["javascript", "python", "java", "react", "node", "sql", "aws", "docker", "html", "css", "typescript", "c++", "c#", "go", "ruby", "php", "backend", "frontend", "api"]
            found_skills = [s for s in common_skills if s in text_lower][:5]
            if not found_skills:
                found_skills = ["Software Development", "Communication", "Problem Solving", "Teamwork"]
                
            profile_data = {
                "job_title": "Software Engineer" if "software" in text_lower or "developer" in text_lower else "Professional",
                "skills": found_skills
            }
        
        # We need a quick mock profile for evaluate_job later
        full_profile = {
            "skills": profile_data['skills'],
            "experience": "See extracted skills.",
            "job_preferences": {"desired_roles": [profile_data['job_title']]}
        }
        
        # Scrape raw jobs from LinkedIn
        raw_jobs = scrape_linkedin_jobs(profile_data['job_title'])
        
        return jsonify({
            "success": True, 
            "profile": full_profile,
            "jobs": raw_jobs
        })
        
    except Exception as e:
        print("Error parsing resume:", e)
        return jsonify({"success": False, "error": str(e)}), 500

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
    
    prompt = f"""
    You are an AI assistant helping a user fill out a job application form.
    Here is the user's master profile:
    {json.dumps(profile, indent=2)}
    
    Here is a list of input fields extracted from the job application webpage:
    {json.dumps(fields, indent=2)}
    
    Determine the best answer for each field based on the user's profile.
    If a field asks for a file upload (like resume), ignore it.
    If you don't know the answer, leave it blank.
    
    Return a JSON object where the keys are the input 'id' (or 'name' if 'id' is missing), 
    and the values are the strings to type into those fields.
    Example: {{"first_name_input": "John", "years_exp": "5"}}
    Return ONLY the valid JSON object.
    """
    
    try:
        response = model.generate_content(prompt)
        text = response.text.strip()
        if text.startswith("```json"): text = text[7:]
        if text.startswith("```"): text = text[3:]
        if text.endswith("```"): text = text[:-3]
            
        answers = json.loads(text)
        return jsonify({"answers": answers})
    except Exception as e:
        print("Error generating answers:", e)
        return jsonify({"answers": {}}), 500

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
    
    for job in found_jobs:
        score, reason = evaluate_job(job['description'], profile)
        if score > 70:
            job['score'] = score
            job['reason'] = reason
            high_match_jobs.append(job)
        time.sleep(4) 
            
    if high_match_jobs:
        send_job_digest(os.getenv("EMAIL_TARGET", "alfrancisbadillapaz10@gmail.com"), high_match_jobs)
        
    return jsonify({"status": "completed", "jobs_found": len(high_match_jobs)})

application = app

if __name__ == '__main__':
    app.run(debug=True, port=5000)
