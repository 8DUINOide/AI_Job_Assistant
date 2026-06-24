import os
import json
import time
import requests
from bs4 import BeautifulSoup
from dotenv import load_dotenv

load_dotenv()

def load_profile():
    with open('master_profile.json', 'r') as f:
        return json.load(f)

def evaluate_job(job, profile):
    desc_lower = job.get('description', '').lower()
    title_lower = job.get('title', '').lower()
    
    skills = [s.lower() for s in profile.get('skills', [])]
    roles = [r.lower() for r in profile.get('job_preferences', {}).get('desired_roles', [])]
    
    score = 20 # Base score
    
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
