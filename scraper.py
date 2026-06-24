import os
import json
import time
import requests
import re
from bs4 import BeautifulSoup
from dotenv import load_dotenv
from tracker import get_applied_job_ids
import pandas as pd
from jobspy import scrape_jobs

load_dotenv()

def load_profile():
    with open('master_profile.json', 'r') as f:
        return json.load(f)

def evaluate_job(job, profile):
    desc_lower = job.get('description', '').lower()
    title_lower = job.get('title', '').lower()
    loc_lower = job.get('location', '').lower()
    
    # Enforce Seniority Rules (Filter out senior roles for fresh grad)
    senior_keywords = ['senior', 'sr', 'lead', 'principal', 'manager', 'director', 'head', 'vp', 'supervisor', 'experienced', 'expert']
    title_words = re.findall(r'\b[a-z]+\b', title_lower.replace('.', ''))
    if any(word in senior_keywords for word in title_words):
        return 0, "Rejected: Senior/Lead role not suitable for fresh graduate."
    
    # Enforce Location Rules
    ph_locations = ['philippines', 'ncr', 'manila', 'makati', 'taguig', 'quezon', 'pasig', 'alabang', 'bicol', 'mandaluyong', 'ortigas']
    is_ph = any(loc in loc_lower for loc in ph_locations)
    is_remote = 'remote' in loc_lower or 'remote' in title_lower or 'remote' in desc_lower
    
    if not is_ph and not is_remote and loc_lower != "unknown":
        return 0, f"Rejected: Outside Philippines and not Remote ({job.get('location', 'Unknown')})."
        
    skills = [s.lower() for s in profile.get('skills', [])]
    roles = [r.lower() for r in profile.get('job_preferences', {}).get('desired_roles', [])]
    
    score = 20 # Base score
    
    # Fresh grad boost
    junior_keywords = ['junior', 'jr', 'entry', 'fresh', 'associate', 'intern', 'trainee', 'graduate']
    if any(word in junior_keywords for word in title_words):
        score += 15
        
    # 1. Role Match (+30 points)
    role_matched = False
    for r in roles:
        if r in title_lower or r in desc_lower:
            score += 30
            role_matched = True
            break
            
    # 2. Skills Match (+10 points per skill, max 50 points)
    matched_skills = [s for s in skills if s in desc_lower or s in title_lower]
    matched_skills = list(set(matched_skills)) # Remove duplicates
    
    skill_points = min(50, len(matched_skills) * 10)
    score += skill_points
    score = min(100, score)
    
    reason = "NLP Matcher: "
    if role_matched:
        reason += "Role matched. "
    reason += f"Found {len(matched_skills)} skills ({', '.join(matched_skills[:4])})."
    
    return score, reason

def evaluate_jobs_batch(jobs, profile):
    results = []
    for job in jobs:
        score, reason = evaluate_job(job, profile)
        results.append({"score": score, "reason": reason})
    return results

def scrape_jobs_multisite(keywords, location="Remote"):
    print(f"Searching for '{keywords}' in '{location}' across multiple sites...")
    
    try:
        jobs_df = scrape_jobs(
            site_name=["linkedin", "indeed"],
            search_term=keywords,
            location=location,
            results_wanted=15,
            country_indeed='USA' # Required to bypass some region blocks
        )
    except Exception as e:
        print(f"Error during jobspy scraping: {e}")
        return []
        
    applied_data = get_applied_job_ids()
    applied_job_ids = applied_data.get('ids', set())
    applied_signatures = applied_data.get('signatures', set())
    
    jobs = []
    
    if jobs_df is None or jobs_df.empty:
        print("No jobs found by jobspy.")
        return jobs
        
    print(f"Found {len(jobs_df)} jobs. Analyzing...")
    
    for idx, row in jobs_df.iterrows():
        if len(jobs) >= 5:
            break
            
        try:
            title = str(row.get('title', 'Unknown'))
            company = str(row.get('company', 'Unknown'))
            location_text = str(row.get('location', 'Unknown'))
            link = str(row.get('job_url', ''))
            description = str(row.get('description', 'No description'))
            
            if pd.isna(row.get('description')):
                description = "No description"
                
            job_id = None
            if 'linkedin.com' in link:
                job_id_match = re.search(r'(\d{9,10})/?$', link.split('?')[0])
                job_id = job_id_match.group(1) if job_id_match else None
            
            signature = f"{company.lower()}|{title.lower()}"
            
            if (job_id and job_id in applied_job_ids) or (signature in applied_signatures):
                print(f"Skipping already applied job: {title} @ {company}")
                continue
                
            jobs.append({
                "title": title,
                "company": company,
                "link": link,
                "location": location_text,
                "description": description
            })
            
        except Exception as e:
            print(f"Error parsing a job row: {e}")
            
    return jobs

if __name__ == "__main__":
    from emailer import send_job_digest
    
    profile = load_profile()
    roles = profile.get('job_preferences', {}).get('desired_roles', ['Software Engineer'])
    search_keyword = roles[0] if roles else "Software Engineer"
    
    found_jobs = scrape_jobs_multisite(search_keyword)
    
    high_match_jobs = []
    
    print("\n--- JOB MATCH RESULTS ---")
    for job in found_jobs:
        score, reason = evaluate_job(job, profile)
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
