import os
import json
import time
import requests
from bs4 import BeautifulSoup
import google.generativeai as genai
from dotenv import load_dotenv

load_dotenv()

os.environ.pop("GOOGLE_APPLICATION_CREDENTIALS", None) # Fix for Google Auth intercepting Gemini API keys
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
model = genai.GenerativeModel('gemini-1.5-flash')

def load_profile():
    with open('master_profile.json', 'r') as f:
        return json.load(f)

def fallback_evaluate_job(job_description, profile):
    desc_lower = job_description.lower()
    skills = [s.lower() for s in profile.get('skills', [])]
    if not skills:
        return 50, "Average match (Fallback Algorithm)"
        
    matched = [s for s in skills if s in desc_lower]
    score = int((len(matched) / len(skills)) * 100) if skills else 50
    
    roles = [r.lower() for r in profile.get('job_preferences', {}).get('desired_roles', [])]
    for r in roles:
        if r in desc_lower:
            score = min(100, score + 20)
            break
            
    reason = f"Fallback AI Algorithm: Matched {len(matched)} skills ({', '.join(matched[:3])})."
    return score, reason

def evaluate_job(job_description, profile):
    prompt = f"""
    You are an expert AI job recruiter. Review the following job description and compare it to the candidate's profile.
    Give a match score out of 100 based on skills, experience, and role.
    Provide a very brief 1-sentence reason for the score.
    
    Candidate Profile (JSON):
    {json.dumps(profile.get('skills', []))}
    {json.dumps(profile.get('experience', []))}
    
    Job Description:
    {job_description[:3000]}
    
    Format your response EXACTLY like this:
    SCORE: [number]
    REASON: [reason]
    """
    try:
        response = model.generate_content(prompt)
        text = response.text
        lines = text.strip().split('\n')
        score = int(lines[0].split(':')[1].strip())
        reason = lines[1].split(':')[1].strip()
        return score, reason
    except Exception as e:
        print(f"Gemini API failed ({e}). Switching to Fallback NLP Algorithm...")
        return fallback_evaluate_job(job_description, profile)

def scrape_linkedin_jobs(keywords, location="Remote"):
    print(f"Searching for '{keywords}' in '{location}' (Easy Apply Only, Serverless Mode)...")
    
    # LinkedIn's public guest API for job search
    search_url = f"https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search?keywords={keywords.replace(' ', '%20')}&location={location}&f_TPR=r86400&f_AL=true&start=0"
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    }
    
    response = requests.get(search_url, headers=headers)
    soup = BeautifulSoup(response.text, 'html.parser')
    
    jobs = []
    job_cards = soup.find_all('li')[:5] # Limit to top 5 for now
    
    print(f"Found {len(job_cards)} jobs. Analyzing...")
    
    for card in job_cards:
        try:
            title_elem = card.find('h3', class_='base-search-card__title')
            company_elem = card.find('h4', class_='base-search-card__subtitle')
            link_elem = card.find('a', class_='base-card__full-link')
            
            if not link_elem:
                continue
                
            title = title_elem.text.strip() if title_elem else "Unknown"
            company = company_elem.text.strip() if company_elem else "Unknown"
            link = link_elem['href'].split('?')[0]
            job_id = link.split('-')[-1]
            
            # Fetch the actual job description HTML
            desc_url = f"https://www.linkedin.com/jobs-guest/jobs/api/jobPosting/{job_id}"
            desc_resp = requests.get(desc_url, headers=headers)
            desc_soup = BeautifulSoup(desc_resp.text, 'html.parser')
            
            desc_elem = desc_soup.find('div', class_='show-more-less-html__markup')
            description = desc_elem.text.strip() if desc_elem else "No description"
            
            jobs.append({
                "title": title,
                "company": company,
                "link": link,
                "description": description
            })
            
            # Sleep briefly to avoid rate limiting, but keep under Vercel 10s limit
            time.sleep(0.5)
            
        except Exception as e:
            print(f"Error parsing a job card: {e}")
            
    return jobs

if __name__ == "__main__":
    from emailer import send_job_digest
    
    profile = load_profile()
    roles = profile.get('job_preferences', {}).get('desired_roles', ['Software Engineer'])
    search_keyword = roles[0] if roles else "Software Engineer"
    
    found_jobs = scrape_linkedin_jobs(search_keyword)
    
    high_match_jobs = []
    
    print("\n--- JOB MATCH RESULTS ---")
    for job in found_jobs:
        score, reason = evaluate_job(job['description'], profile)
        print(f"\nRole: {job['title']} @ {job['company']}")
        print(f"Link: {job['link']}")
        print(f"Match Score: {score}/100")
        print(f"Reason: {reason}")
        
        if score > 70:
            print(">>> HIGH MATCH! You should apply for this.")
            job['score'] = score
            job['reason'] = reason
            high_match_jobs.append(job)
            
        time.sleep(5)
            
    if high_match_jobs:
        print("\nSending email digest...")
        send_job_digest("alfrancisbadillapaz10@gmail.com", high_match_jobs)
