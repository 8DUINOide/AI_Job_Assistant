import os
import sys
import json

# Add parent directory to path so we can import our other modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from flask import Flask, request, jsonify
from flask_cors import CORS
import google.generativeai as genai
from dotenv import load_dotenv

# Import our custom modules
from tracker import log_application
from scraper import load_profile, scrape_linkedin_jobs, evaluate_job
from emailer import send_job_digest
import time

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
model = genai.GenerativeModel('gemini-2.5-flash')

app = Flask(__name__)
CORS(app)

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

@app.route('/api/cron', methods=['GET', 'POST'])
def run_scraper_cron():
    """This endpoint is triggered daily by Vercel Cron."""
    # Verify secure cron token (optional but recommended in production)
    auth_header = request.headers.get('Authorization')
    if os.getenv("CRON_SECRET") and auth_header != f"Bearer {os.getenv('CRON_SECRET')}":
        return jsonify({"error": "Unauthorized"}), 401

    profile = load_profile()
    roles = profile.get('job_preferences', {}).get('desired_roles', ['Software Engineer'])
    search_keyword = roles[0] if roles else "Software Engineer"
    
    # Scrape only 3 pages (approx 15 jobs) to stay within Vercel execution timeouts
    found_jobs = scrape_linkedin_jobs(search_keyword)
    
    high_match_jobs = []
    
    for job in found_jobs:
        score, reason = evaluate_job(job['description'], profile)
        if score > 70:
            job['score'] = score
            job['reason'] = reason
            high_match_jobs.append(job)
        time.sleep(4) # Respect Gemini RPM limits
            
    if high_match_jobs:
        send_job_digest(os.getenv("EMAIL_TARGET", "alfrancisbadillapaz10@gmail.com"), high_match_jobs)
        
    return jsonify({"status": "completed", "jobs_found": len(high_match_jobs)})

# Export the app for Vercel
# Vercel needs an application instance
application = app
