import os
import json
import PyPDF2
from playwright.sync_api import sync_playwright
import google.generativeai as genai
from dotenv import load_dotenv

load_dotenv()

# Configure Gemini
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
model = genai.GenerativeModel('gemini-2.5-flash')

def extract_text_from_pdf(pdf_path):
    """Reads all text from a PDF file."""
    text = ""
    try:
        with open(pdf_path, 'rb') as file:
            reader = PyPDF2.PdfReader(file)
            for page in reader.pages:
                text += page.extract_text() + "\n"
        return text
    except Exception as e:
        print(f"Error reading PDF: {e}")
        return ""

def extract_text_from_url(url):
    """Uses Playwright to grab the text content of a portfolio website."""
    text = ""
    print(f"Scanning portfolio URL: {url}...")
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            page.goto(url)
            # wait a moment for javascript to load
            page.wait_for_timeout(2000)
            text = page.evaluate("document.body.innerText")
            browser.close()
        return text
    except Exception as e:
        print(f"Error scanning URL: {e}")
        return ""

def build_master_profile(resume_text, portfolio_text):
    """Uses Gemini to generate the master_profile.json structure."""
    print("Sending data to Gemini AI to build your master profile...")
    
    prompt = f"""
    You are an expert technical recruiter and career coach.
    I will provide you with the raw text from my Resume/LinkedIn and my Portfolio website.
    I want you to analyze my experience, skills, and background, and generate a structured JSON profile.
    
    Based on the data, determine my 'desired_roles' (e.g., Backend Developer, Full Stack Developer, Tech Lead).
    Do NOT include frontend roles if my primary experience is backend/fullstack unless specifically stated.
    
    Raw Resume/LinkedIn Text:
    {resume_text}
    
    Raw Portfolio Text:
    {portfolio_text}
    
    Respond ONLY with a valid JSON object matching exactly this schema:
    {{
      "personal_info": {{
        "first_name": "...",
        "last_name": "...",
        "email": "...",
        "phone": "...",
        "location": "...",
        "linkedin_url": "...",
        "portfolio_url": "..."
      }},
      "job_preferences": {{
        "desired_roles": ["Role 1", "Role 2"],
        "work_type": ["Remote", "Hybrid", "On-site"],
        "locations": ["...", "Remote"],
        "salary_expectation": "..."
      }},
      "summary": "A professional summary of my background based on the data...",
      "experience": [
        {{
          "title": "...",
          "company": "...",
          "start_date": "...",
          "end_date": "...",
          "description": "...",
          "skills_used": ["..."]
        }}
      ],
      "education": [
        {{
          "degree": "...",
          "university": "...",
          "graduation_year": "..."
        }}
      ],
      "skills": ["Skill 1", "Skill 2"]
    }}
    """
    
    try:
        response = model.generate_content(prompt)
        response_text = response.text.strip()
        
        # Clean up any markdown blocks around the json
        if response_text.startswith("```json"):
            response_text = response_text[7:]
        if response_text.startswith("```"):
            response_text = response_text[3:]
        if response_text.endswith("```"):
            response_text = response_text[:-3]
            
        profile_data = json.loads(response_text)
        
        with open('master_profile.json', 'w') as f:
            json.dump(profile_data, f, indent=2)
            
        print("Successfully generated and saved 'master_profile.json'!")
        print("Determined desired roles:", profile_data['job_preferences']['desired_roles'])
        
    except Exception as e:
        print(f"Error generating profile: {e}")
        print("Raw response was:", response.text)

if __name__ == "__main__":
    resume_file = "resume.pdf" # Place your resume here
    portfolio_url = "https://8duinoide.github.io/portfolio/"
    
    print(f"Looking for '{resume_file}'...")
    if os.path.exists(resume_file):
        resume_text = extract_text_from_pdf(resume_file)
        portfolio_text = extract_text_from_url(portfolio_url) if "YOUR_" not in portfolio_url else ""
        
        build_master_profile(resume_text, portfolio_text)
    else:
        print(f"Could not find '{resume_file}'. Please place your resume in the folder and run again.")
