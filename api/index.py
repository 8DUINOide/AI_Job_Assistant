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
from resume_generator import get_tailored_profile_data, generate_pdf_from_data

load_dotenv()

app = Flask(__name__)
CORS(app)

ADMIN_EMAIL = 'admin@gmail.com'
ADMIN_PASSWORD = '12345678'
ADMIN_TOKEN = 'secret_admin_token_123_abc_xyz'

from functools import wraps
from flask import request, jsonify

def require_auth(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({"success": False, "error": "Unauthorized"}), 401
        
        token = auth_header.split(' ')[1]
        if token != ADMIN_TOKEN:
            return jsonify({"success": False, "error": "Unauthorized"}), 401
            
        return f(*args, **kwargs)
    return decorated

@app.route('/api/login', methods=['POST'])
def login():
    data = request.json or {}
    email = data.get('email')
    password = data.get('password')
    
    if email == ADMIN_EMAIL and password == ADMIN_PASSWORD:
        return jsonify({"success": True, "token": ADMIN_TOKEN})
    
    return jsonify({"success": False, "error": "Invalid credentials"}), 401

# -----------------
# 1. PUBLIC WEB APP
# -----------------
@app.route('/')
def home():
    """Serves the beautiful web app homepage."""
    # Vercel sets the CWD to the project root
    return send_file(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'index.html'))

@app.route('/api/profile', methods=['GET'])
@require_auth
def get_profile():
    try:
        profile = load_profile()
        return jsonify({"success": True, "profile": profile})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/build-profile', methods=['POST'])
@require_auth
def build_profile_endpoint():
    try:
        portfolio_url = request.form.get('portfolio_url', '')
        file = request.files.get('file')
        
        if not file:
            return jsonify({"success": False, "error": "No resume file provided"}), 400
        
        # Extract text from PDF inline (avoids profile_builder.py playwright dependency)
        resume_text = ""
        try:
            reader = PyPDF2.PdfReader(file)
            for page in reader.pages:
                page_text = page.extract_text()
                if page_text:
                    resume_text += page_text + "\n"
        except Exception as pdf_err:
            return jsonify({"success": False, "error": f"Failed to read PDF: {str(pdf_err)}"}), 400
        
        if not resume_text.strip():
            return jsonify({"success": False, "error": "Could not extract any text from the PDF"}), 400
        
        # Portfolio URL scraping (best-effort, skip if unavailable)
        portfolio_text = ""
        if portfolio_url:
            try:
                import requests as req
                resp = req.get(portfolio_url, timeout=5)
                if resp.ok:
                    from bs4 import BeautifulSoup
                    soup = BeautifulSoup(resp.text, 'html.parser')
                    portfolio_text = soup.get_text(separator='\n', strip=True)
            except Exception:
                portfolio_text = ""
        
        # --- Smart Local Heuristic Parser (No Gemini API to avoid fees) ---
        import re
        from resume_generator import COMMON_KEYWORDS

        full_text = resume_text + "\n" + portfolio_text
        text_lower = full_text.lower()

        # 1. Contact Info Extraction via Regex (tolerant of spaces from PDF parsing)
        email_match = re.search(r'[a-zA-Z0-9._%+-]+\s*@\s*[a-zA-Z0-9.-]+\s*\.\s*[a-zA-Z]{2,}', full_text)
        phone_match = re.search(r'\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4}', full_text)
        linkedin_match = re.search(r'(?:https?://)?(?:www\.)?linkedin\.com/in/[\w-]+', full_text)

        email = email_match.group(0).replace(' ', '').replace('\n', '') if email_match else ""
        phone = phone_match.group(0) if phone_match else ""
        linkedin_url = linkedin_match.group(0) if linkedin_match else ""

        # 2. Name Heuristic (Usually on the first non-empty line)
        lines = [line.strip() for line in resume_text.split('\n') if line.strip()]
        first_name = ""
        last_name = ""
        if lines:
            name_parts = lines[0].split()
            if len(name_parts) >= 2:
                first_name = name_parts[0]
                last_name = " ".join(name_parts[1:])
            else:
                first_name = name_parts[0] if name_parts else ""
                
        # 2.5 Location Extraction Heuristic (look in first 15 lines for City, Region format)
        location = ""
        for line in lines[:15]:
            # A line is likely a location if it has a comma, is short, and isn't an email/link
            if ',' in line and len(line) < 40 and '@' not in line and 'http' not in line:
                # Also ensure it doesn't look like a date range or degree
                if not any(x in line.lower() for x in ['university', 'college', 'school', 'jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec']):
                    location = line.strip()
                    break

        # 3. Skills Extraction
        found_skills = list(set([skill.title() for skill in COMMON_KEYWORDS if skill.lower() in text_lower]))

        # 4. Text Segmentation for Experience, Education, Summary
        sections = {'experience': '', 'education': '', 'summary': '', 'certifications': '', 'projects': '', 'skills': ''}
        current_section = None
        
        for line in resume_text.split('\n'):
            line_clean = line.strip().upper().replace(":", "")
            if not line_clean:
                continue
                
            if len(line_clean) < 40 and not line.strip().startswith(('-', '•', '●', '*', '▪', '»', '➢', '')):
                if any(h in line_clean for h in ['EXPERIENCE', 'EMPLOYMENT', 'WORK HISTORY', 'ACTIVITIES', 'INVOLVEMENT', 'LEADERSHIP']):
                    current_section = 'experience'
                    continue
                elif any(h in line_clean for h in ['EDUCATION', 'ACADEMIC']):
                    current_section = 'education'
                    continue
                elif any(h in line_clean for h in ['SUMMARY', 'PROFILE', 'ABOUT ME']):
                    current_section = 'summary'
                    continue
                elif any(h in line_clean for h in ['CERTIFICAT', 'LICENSES', 'AWARD', 'COURSE']):
                    current_section = 'certifications'
                    continue
                elif any(h in line_clean for h in ['PROJECT', 'PORTFOLIO']):
                    current_section = 'projects'
                    continue
                elif any(h in line_clean for h in ['SKILL', 'TECHNOLOGIES', 'EXPERTISE', 'ADDITIONAL']):
                    current_section = 'skills'
                    continue
                
            if current_section and current_section in sections:
                sections[current_section] += line.strip() + "\n"

        # Build Experience Array
        experiences = []
        if sections['experience'].strip():
            exp_lines = [l for l in sections['experience'].split('\n') if l.strip()]
            
            import re
            date_regex = re.compile(r'((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s*\d{4}|\b\d{4}\b).*?(?:-|to|–).*?(Present|Current|\b\d{4}\b|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s*\d{4})', re.IGNORECASE)
            
            date_indices = []
            for idx, line in enumerate(exp_lines):
                if date_regex.search(line):
                    date_indices.append(idx)
                    
            for i, d_idx in enumerate(date_indices):
                if i == 0:
                    start_header = 0
                else:
                    prev_d_idx = date_indices[i-1]
                    start_header = d_idx
                    for _ in range(2):
                        if start_header > prev_d_idx + 1:
                            prev_line = exp_lines[start_header - 1]
                            if prev_line.strip().startswith(('-', '•', '●', '*', '▪', '»', '➢', '')) or len(prev_line) > 80:
                                break
                            start_header -= 1
                    
                    desc_lines = exp_lines[prev_d_idx+1 : start_header]
                    if experiences:
                        experiences[-1]['description'] = "\n".join(desc_lines)
                        
                header_lines = exp_lines[start_header : d_idx]
                date_line = exp_lines[d_idx]
                date_match = date_regex.search(date_line)
                start_date = date_match.group(1).strip() if date_match.group(1) else ""
                end_date = date_match.group(2).strip() if date_match.group(2) else ""
                
                date_text_before = date_line[:date_match.start()].strip()
                if date_text_before:
                    header_lines.append(date_text_before)
                
                company = header_lines[0] if len(header_lines) > 0 else "Unknown Company"
                title = header_lines[1] if len(header_lines) > 1 else company
                if len(header_lines) == 1:
                    if " at " in company.lower():
                        parts = re.split(r'\s+at\s+', company, flags=re.IGNORECASE)
                        title, company = parts[0], parts[1]
                    elif "," in company:
                        title, company = company.split(",", 1)
                
                experiences.append({
                    "title": title.strip()[:100],
                    "company": company.strip()[:100],
                    "start_date": start_date,
                    "end_date": end_date,
                    "description": "",
                    "skills_used": found_skills[:5]
                })

            if date_indices and experiences:
                last_d_idx = date_indices[-1]
                desc_lines = exp_lines[last_d_idx+1:]
                experiences[-1]['description'] = "\n".join(desc_lines)
                
            if not experiences:
                # Fallback if no dates found
                chunk_size = 5
                for i in range(0, len(exp_lines), chunk_size):
                    chunk = exp_lines[i:i+chunk_size]
                    title = chunk[0] if chunk else "Experience"
                    desc = "\n".join(chunk[1:]) if len(chunk) > 1 else title
                    experiences.append({
                        "title": title[:100],
                        "company": "See Description",
                        "start_date": "",
                        "end_date": "",
                        "description": desc,
                        "skills_used": found_skills[:5]
                    })

        # Build Education Array
        educations = []
        if sections['education'].strip():
            edu_lines = [l for l in sections['education'].split('\n') if l.strip()]
            import re
            year_regex = re.compile(r'\b(19|20)\d{2}\b')
            
            blocks = []
            current_block = []
            for line in edu_lines:
                line_lower = line.lower()
                if any(kw in line_lower for kw in ['university', 'college', 'institute', 'polytechnic']):
                    if len(current_block) >= 2:
                        blocks.append(current_block)
                        current_block = []
                current_block.append(line)
            if current_block:
                blocks.append(current_block)
                
            for block in blocks:
                current_uni_parts = []
                current_degree_parts = []
                current_year = ""
                
                for line in block:
                    year_match = year_regex.search(line)
                    if year_match and not current_year:
                        current_year = year_match.group(0)
                        
                    line_lower = line.lower()
                    if any(kw in line_lower for kw in ['university', 'college', 'institute', 'school', 'academy', 'polytechnic']):
                        current_uni_parts.append(line)
                    elif any(kw in line_lower for kw in ['degree', 'bachelor', 'master', 'phd', 'bs ', 'ba ', 'ms ', 'b.s.', 'm.s.', 'major', 'minor', 'gwa', 'gpa', 'laude']):
                        current_degree_parts.append(line)
                    else:
                        if current_uni_parts and not current_degree_parts:
                            current_degree_parts.append(line)
                        elif not current_uni_parts:
                            current_uni_parts.append(line)
                        else:
                            current_degree_parts.append(line)
                
                if not current_uni_parts and not current_degree_parts:
                    if len(block) > 0:
                        current_uni_parts = [block[0]]
                    if len(block) > 1:
                        current_degree_parts = block[1:]
                elif not current_uni_parts and current_degree_parts:
                    if len(current_degree_parts) > 1:
                        current_uni_parts = [current_degree_parts.pop(0)]
                elif not current_degree_parts and current_uni_parts:
                    if len(current_uni_parts) > 1:
                        current_degree_parts = current_uni_parts[1:]
                        current_uni_parts = [current_uni_parts[0]]

                university = " - ".join(current_uni_parts)
                degree = " - ".join(current_degree_parts)
                
                if not university and degree:
                    university = degree
                    degree = ""

                educations.append({
                    "university": university.strip()[:150],
                    "degree": degree.strip()[:300],
                    "graduation_year": current_year
                })

        summary = sections['summary'].strip()
        if not summary:
            summary = f"Professional with expertise in {', '.join(found_skills[:5])}."
            
        certifications = [l.strip() for l in sections['certifications'].split('\n') if l.strip()]
        if not certifications:
            import re
            cert_match = re.search(r'(?i)(?:Certifications|Certificates|Licenses)\s*[:\-]?\s*(.+)', full_text)
            if cert_match:
                cert_text = cert_match.group(1)
                cert_text = re.split(r'[\·\|]|Languages:', cert_text)[0].strip()
                certifications = [c.strip() for c in re.split(r'[,;]', cert_text) if c.strip()]

        projects = []
        if sections.get('projects', '').strip():
            proj_lines = [l for l in sections['projects'].split('\n') if l.strip()]
            import re
            date_regex_proj = re.compile(r'\b(19|20)\d{2}\b')
            
            date_indices = []
            for idx, line in enumerate(proj_lines):
                if date_regex_proj.search(line) and not line.strip().startswith(('-', '•', '●', '*', '▪', '»', '➢', '')):
                    date_indices.append(idx)
                    
            if len(date_indices) > 0:
                for i, d_idx in enumerate(date_indices):
                    if i == 0:
                        start_header = 0
                    else:
                        prev_d_idx = date_indices[i-1]
                        start_header = d_idx
                        for _ in range(2):
                            if start_header > prev_d_idx + 1:
                                prev_line = proj_lines[start_header - 1]
                                if prev_line.strip().startswith(('-', '•', '●', '*', '▪', '»', '➢', '')) or len(prev_line) > 80:
                                    break
                                start_header -= 1
                        
                        desc_lines = proj_lines[prev_d_idx+1 : start_header]
                        if projects:
                            projects[-1]['description'] = "\n".join(desc_lines)
                    
                    date_line = proj_lines[d_idx]
                    date_match = date_regex_proj.search(date_line)
                    start_date = date_match.group(0).strip() if date_match else ""
                    end_date = ""
                    
                    header_lines = proj_lines[start_header : d_idx]
                    date_text_before = date_line[:date_match.start()].strip() if date_match else ""
                    if date_text_before:
                        header_lines.append(date_text_before)
                        
                    title = header_lines[0] if len(header_lines) > 0 else "Unknown Project"
                    if len(header_lines) > 1:
                        title = " - ".join(header_lines)
                        
                    projects.append({
                        "title": title.strip()[:100],
                        "dates": start_date,
                        "role": "",
                        "link": "",
                        "description": ""
                    })

                if date_indices and projects:
                    last_d_idx = date_indices[-1]
                    desc_lines = proj_lines[last_d_idx+1:]
                    projects[-1]['description'] = "\n".join(desc_lines)
            else:
                current_proj = []
                for line in proj_lines:
                    if not line.strip().startswith(('-', '•', '●', '*', '▪', '»', '➢', '')) and any(l.strip().startswith(('-', '•', '●', '*', '▪', '»', '➢', '')) for l in current_proj):
                        if current_proj:
                            title = current_proj[0]
                            desc = "\n".join(current_proj[1:]) if len(current_proj) > 1 else title
                            projects.append({"title": title[:100], "dates": "", "role": "", "link": "", "description": desc})
                            current_proj = []
                    current_proj.append(line)
                if current_proj:
                    title = current_proj[0]
                    desc = "\n".join(current_proj[1:]) if len(current_proj) > 1 else title
                    projects.append({"title": title[:100], "dates": "", "role": "", "link": "", "description": desc})

        profile_data = {
            "personal_info": {
                "first_name": first_name,
                "last_name": last_name,
                "email": email,
                "phone": phone,
                "location": location,
                "linkedin_url": linkedin_url,
                "portfolio_url": portfolio_url
            },
            "job_preferences": {
                "desired_roles": ["Software Engineer"], # Override by app
                "work_type": ["Remote", "Hybrid"],
                "locations": ["Remote"],
                "salary_expectation": ""
            },
            "summary": summary if summary else "Profile automatically extracted from uploaded resume.",
            "experience": experiences,
            "education": educations,
            "skills": found_skills,
            "certifications": certifications,
            "projects": projects
        }
        
        # Also save to master_profile.json for the web dashboard
        try:
            profile_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'master_profile.json')
            with open(profile_path, 'w') as f:
                json.dump(profile_data, f, indent=2)
        except Exception:
            pass  # Non-critical, Vercel filesystem is read-only anyway
            
        return jsonify({"success": True, "profile": profile_data})
    except Exception as e:
        print("Error in build_profile:", e)
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/logs', methods=['GET'])
@require_auth
def get_logs():
    try:
        logs = get_recent_logs(limit=200)
        if isinstance(logs, dict) and logs.get("auth_error"):
            return jsonify({"success": False, "auth_error": True, "error": "Google Sheets Authentication failed."}), 401
        return jsonify({"success": True, "logs": logs})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

def normalize_profile(p):
    """Converts Android camelCase profile to Python snake_case profile."""
    if not p: return p
    norm = {}
    
    pi = p.get('personalInfo') or p.get('personal_info') or {}
    norm['personal_info'] = {
        'first_name': pi.get('firstName') or pi.get('first_name', ''),
        'last_name': pi.get('lastName') or pi.get('last_name', ''),
        'email': pi.get('email', ''),
        'phone': pi.get('phone', ''),
        'location': pi.get('location', ''),
        'linkedin_url': pi.get('linkedinUrl') or pi.get('linkedin_url', ''),
        'portfolio_url': pi.get('portfolioUrl') or pi.get('portfolio_url', '')
    }
    
    jp = p.get('jobPreferences') or p.get('job_preferences') or {}
    norm['job_preferences'] = {
        'desired_roles': jp.get('desiredRoles') or jp.get('desired_roles', []),
        'work_type': jp.get('workType') or jp.get('work_type', []),
        'locations': jp.get('locations', []),
        'salary_expectation': jp.get('salaryExpectation') or jp.get('salary_expectation', '')
    }
    
    norm['summary'] = p.get('summary', '')
    norm['skills'] = p.get('skills', [])
    
    exps = p.get('experience', [])
    norm['experience'] = []
    for e in exps:
        norm['experience'].append({
            'title': e.get('title', ''),
            'company': e.get('company', ''),
            'location': e.get('location', ''),
            'start_date': e.get('startDate') or e.get('start_date', ''),
            'end_date': e.get('endDate') or e.get('end_date', ''),
            'description': e.get('description', ''),
            'skills_used': e.get('skillsUsed') or e.get('skills_used', [])
        })
        
    edus = p.get('education', [])
    norm['education'] = []
    for e in edus:
        norm['education'].append({
            'degree': e.get('degree', ''),
            'university': e.get('university', ''),
            'graduation_year': e.get('graduationYear') or e.get('graduation_year', ''),
            'gpa': e.get('gpa', '')
        })
        
    projs = p.get('projects', [])
    norm['projects'] = []
    for proj in projs:
        norm['projects'].append({
            'title': proj.get('title', ''),
            'role': proj.get('role', ''),
            'dates': proj.get('dates', ''),
            'description': proj.get('description', ''),
            'link': proj.get('link', '')
        })
        
    norm['certifications'] = p.get('certifications', [])
    norm['awards'] = p.get('awards', [])
        
    return norm

@app.route('/api/run-agent-manually', methods=['POST'])
@require_auth
def run_agent_manually():
    """Fetches raw jobs based on master profile to bypass Vercel timeout."""
    try:
        data = request.json or {}
        profile = normalize_profile(data.get('profile')) or load_profile()
        roles = profile.get('job_preferences', {}).get('desired_roles', ['Software Engineer'])
        
        search_keyword = data.get('search_keyword')
        if not search_keyword:
            search_keyword = roles[0] if roles else "Software Engineer"
            
        location = data.get('location', 'Remote')
        offset = data.get('offset', 0)
        results_wanted = data.get('results_wanted', 30)
        
        # Scrape raw jobs from multiple platforms
        raw_jobs = scrape_jobs_multisite(search_keyword, location=location, offset=offset, results_wanted=results_wanted)
        
        return jsonify({
            "success": True, 
            "profile": profile,
            "jobs": raw_jobs
        })
    except Exception as e:
        print("Error running agent manually:", e)
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/evaluate-jobs', methods=['POST'])
@require_auth
def evaluate_multiple_jobs():
    """Evaluates multiple jobs in a single batch to bypass Gemini RPM limits and speed up execution."""
    data = request.json
    jobs = data.get('jobs', [])
    jobs = data.get('jobs', [])
    profile = normalize_profile(data.get('profile'))
    
    if not jobs or not profile:
        return jsonify({"success": False, "error": "Missing data"}), 400
        
    evaluated_results = evaluate_jobs_batch(jobs, profile)
    
    for i, job in enumerate(jobs):
        if i < len(evaluated_results):
            job['score'] = evaluated_results[i].get('score', 0)
            job['reason'] = evaluated_results[i].get('reason', 'Evaluation failed')
            job['tech_stack'] = evaluated_results[i].get('tech_stack', '')
        else:
            job['score'] = 50
            job['reason'] = 'Evaluation parsing error'
            job['tech_stack'] = ''
            
    return jsonify({"success": True, "jobs": jobs})

@app.route('/api/evaluate-job', methods=['POST'])
@require_auth
def evaluate_single_job():
    """Evaluates a single job to bypass Vercel 10s limit."""
    data = request.json
    job = data.get('job')
    job = data.get('job')
    profile = normalize_profile(data.get('profile'))
    
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
@require_auth
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
            now = datetime.datetime.now()
            today = f"{now.strftime('%B')} {now.day}, {now.year}"
            rows = []
            for j in jobs:
                rows.append({
                    'company': j.get('company', 'Unknown'),
                    'job_title': j.get('title', 'Unknown'),
                    'tech_stack': j.get('tech_stack', ''),
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
@require_auth
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
@require_auth
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
@require_auth
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
@require_auth
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
                    job['tech_stack'] = evaluated_results[i].get('tech_stack', '')
                    high_match_jobs.append(job)
            
    if high_match_jobs:
        send_job_digest(os.getenv("EMAIL_TARGET", "alfrancisbadillapaz10@gmail.com"), high_match_jobs)
        
        # Log these jobs as pending
        import datetime
        now = datetime.datetime.now()
        today = f"{now.strftime('%B')} {now.day}, {now.year}"
        rows = []
        for j in high_match_jobs:
            rows.append({
                'company': j.get('company', 'Unknown'),
                'job_title': j.get('title', 'Unknown'),
                'tech_stack': j.get('tech_stack', ''),
                'status': 'Pending',
                'date_applied': today,
                'job_link': j.get('link', ''),
                'location': j.get('location', ''),
                'salary': j.get('salary', ''),
                'contact_person': j.get('contact_person', '')
            })
        log_applications_batch(rows)
        
    return jsonify({"status": "completed", "jobs_found": len(high_match_jobs)})

@app.route('/api/generate-resume', methods=['POST'])
@require_auth
def generate_resume():
    try:
        data = request.json
        job_description = data.get('job_description')
        if not job_description:
            return jsonify({"success": False, "error": "Job description is required"}), 400
            
        master_profile = normalize_profile(data.get('profile')) or load_profile()
        tailored_data = get_tailored_profile_data(master_profile, job_description)
        
        if not tailored_data:
            return jsonify({"success": False, "error": "Failed to tailor profile using Gemini."}), 500
            
        pdf_buffer = generate_pdf_from_data(tailored_data)
        
        return send_file(
            pdf_buffer,
            as_attachment=True,
            download_name='Tailored_Resume.pdf',
            mimetype='application/pdf'
        )
    except Exception as e:
        print("Error generating resume:", e)
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/analyze-resume', methods=['POST'])
@require_auth
def analyze_resume():
    """Analyzes JD and returns match rate, keywords, and editable tailored data."""
    try:
        data = request.json
        job_description = data.get('job_description')
        if not job_description:
            return jsonify({"success": False, "error": "Job description is required"}), 400
            
        master_profile = normalize_profile(data.get('profile')) or load_profile()
        
        # 1. Match Rate
        # We reuse evaluate_job which takes a dict with description
        from scraper import evaluate_job
        score, reason = evaluate_job({'description': job_description, 'title': '', 'location': 'Unknown'}, master_profile)
        
        # 2. Keywords Extraction
        from resume_generator import extract_keywords
        profile_skills = master_profile.get("skills", [])
        keywords_to_include, missing_keywords, matched_skills = extract_keywords(job_description, profile_skills)
        
        # 3. Base Tailored Data
        tailored_data = get_tailored_profile_data(master_profile, job_description)
        
        # 4. Cover Letter Text
        from resume_generator import generate_cover_letter_text
        cover_letter_text = generate_cover_letter_text(master_profile, job_description)
        
        return jsonify({
            "success": True,
            "match_rate": score,
            "keywords_to_include": keywords_to_include,
            "missing_keywords": missing_keywords,
            "matched_skills": matched_skills,
            "tailored_data": tailored_data,
            "cover_letter_text": cover_letter_text
        })
    except Exception as e:
        print("Error analyzing resume:", e)
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/generate-pdf', methods=['POST'])
@require_auth
def generate_pdf():
    """Generates PDF directly from user-edited tailored data."""
    try:
        data = request.json
        tailored_data = data.get('tailored_data')
        if not tailored_data:
            return jsonify({"success": False, "error": "Tailored data is required"}), 400
            
        from resume_generator import convert_profile_to_pdf_data
        pdf_data = convert_profile_to_pdf_data(tailored_data)
        pdf_buffer = generate_pdf_from_data(pdf_data)
        
        first_name = tailored_data.get("personal_info", {}).get("first_name", "Applicant")
        last_name = tailored_data.get("personal_info", {}).get("last_name", "")
        name = f"{first_name} {last_name}".strip()
        
        return send_file(
            pdf_buffer,
            as_attachment=True,
            download_name=f'{name}_Resume.pdf',
            mimetype='application/pdf'
        )
    except Exception as e:
        print("Error generating PDF:", e)
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/generate-cover-letter-pdf', methods=['POST'])
@require_auth
def generate_cover_letter_pdf():
    """Generates Cover Letter PDF from text."""
    try:
        data = request.json
        cover_letter_text = data.get('cover_letter_text')
        if not cover_letter_text:
            return jsonify({"success": False, "error": "Cover letter text is required"}), 400
            
        master_profile = load_profile()
        from resume_generator import generate_cover_letter_pdf_from_text
        pdf_buffer = generate_cover_letter_pdf_from_text(cover_letter_text, master_profile)
        
        first_name = master_profile.get("personal_info", {}).get("first_name", "Applicant")
        last_name = master_profile.get("personal_info", {}).get("last_name", "")
        name = f"{first_name} {last_name}".strip()
        
        return send_file(
            pdf_buffer,
            as_attachment=True,
            download_name=f'{name}_Cover Letter.pdf',
            mimetype='application/pdf'
        )
    except Exception as e:
        print("Error generating cover letter PDF:", e)
        return jsonify({"success": False, "error": str(e)}), 500

application = app

if __name__ == '__main__':
    app.run(debug=True, port=5000)
