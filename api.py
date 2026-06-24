import json
import os
from flask import Flask, request, jsonify
from flask_cors import CORS
import google.generativeai as genai
from dotenv import load_dotenv
from tracker import log_application

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
model = genai.GenerativeModel('gemini-2.5-flash')

app = Flask(__name__)
CORS(app) # Allow the Chrome extension to make requests

def load_profile():
    with open('master_profile.json', 'r') as f:
        return json.load(f)

@app.route('/api/fill-form', methods=['POST'])
def fill_form():
    data = request.json
    fields = data.get('fields', [])
    profile = load_profile()
    
    print(f"Received request to fill form with {len(fields)} fields.")
    
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
        
        # Clean markdown if present
        if text.startswith("```json"): text = text[7:]
        if text.startswith("```"): text = text[3:]
        if text.endswith("```"): text = text[:-3]
            
        answers = json.loads(text)
        print("Generated answers:", answers)
        return jsonify({"answers": answers})
        
    except Exception as e:
        print("Error generating answers:", e)
        return jsonify({"answers": {}}), 500

@app.route('/api/log-job', methods=['POST'])
def log_job():
    data = request.json
    url = data.get('url', '')
    title = data.get('title', 'Unknown Job')
    
    print(f"Logging job: {title} ({url})")
    # Parse title simply. Most pages are "Job Title at Company | LinkedIn"
    parts = title.split(' at ')
    job_name = parts[0].strip() if len(parts) > 1 else title
    company = parts[1].split('|')[0].strip() if len(parts) > 1 else "Unknown"
    
    import datetime
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    
    success = log_application(job_name, company, today, "Applied via Extension", url)
    if success:
        return jsonify({"success": True})
    return jsonify({"success": False}), 500

if __name__ == '__main__':
    print("Starting AI Job Assistant Backend on port 5000...")
    app.run(port=5000)
