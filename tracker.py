import os
import json
import re
from google.oauth2 import service_account
from googleapiclient.discovery import build

# If modifying these scopes, delete the file token.json.
SCOPES = ['https://www.googleapis.com/auth/spreadsheets']

# This is a placeholder. We will need the actual Spreadsheet ID from your "Job Hunting" sheet's URL.
# Example: https://docs.google.com/spreadsheets/d/YOUR_SPREADSHEET_ID/edit
SPREADSHEET_ID = '1-9st_OvzdkFf8qFAek5fCKnm1ta1UqRXpejAXW0IJdY'
RANGE_NAME = 'Sheet1!A:I'  # Adjusted to fetch all 9 columns

def get_sheets_service():
    """Authenticates and returns the Google Sheets API service."""
    creds = None
    
    # 1. Try to load from Vercel Environment Variable first
    google_creds_json = os.getenv('GOOGLE_CREDENTIALS')
    if google_creds_json:
        import json
        creds_dict = json.loads(google_creds_json)
        creds = service_account.Credentials.from_service_account_info(
            creds_dict, scopes=SCOPES)
    else:
        # 2. Fallback to local file for local testing
        creds_file = os.path.join(os.path.dirname(__file__), 'credentials.json')
        if not os.path.exists(creds_file):
            raise FileNotFoundError("Missing Google Credentials. Set GOOGLE_CREDENTIALS env var or create credentials.json.")
        creds = service_account.Credentials.from_service_account_file(
            creds_file, scopes=SCOPES)

    service = build('sheets', 'v4', credentials=creds)
    return service

def log_application(job_title, company, date_applied, status, job_link):
    """Appends a new row to the Job Hunting spreadsheet."""
    try:
        service = get_sheets_service()
        
        # The data to insert (Aligned with user's columns)
        # Columns: Company Name, Role Title, Tech Stack, Status, Application Date, Job Post Link, Contact Person, Salary / Package, Location
        values = [
            [company, job_title, "", status, date_applied, job_link, "", "", ""]
        ]
        body = {
            'values': values
        }
        
        result = service.spreadsheets().values().append(
            spreadsheetId=SPREADSHEET_ID,
            range=RANGE_NAME,
            valueInputOption='USER_ENTERED',
            insertDataOption='INSERT_ROWS',
            body=body
        ).execute()

        print(f"[{company}] '{job_title}' successfully logged to spreadsheet!")
        return True
    
    except Exception as e:
        print(f"Error logging to spreadsheet: {e}")
        return False

def update_application_status(company, job_title, new_status):
    """Updates the status of an existing application in the spreadsheet."""
    try:
        service = get_sheets_service()
        
        # Find the row index
        result = service.spreadsheets().values().get(
            spreadsheetId=SPREADSHEET_ID,
            range=RANGE_NAME
        ).execute()
        
        values = result.get('values', [])
        row_index = -1
        for i, row in enumerate(values):
            if len(row) > 1:
                r_company = row[0].strip().lower()
                r_title = row[1].strip().lower()
                if r_company == company.lower() and r_title == job_title.lower():
                    row_index = i + 1 # Sheets are 1-indexed
                    break
        
        if row_index == -1:
            print(f"Could not find [{company}] '{job_title}' to update status.")
            return False
            
        import datetime
        today = datetime.datetime.now().strftime("%Y-%m-%d")
        
        # If applying, set status and today's date. If skipping, set status and clear date.
        if new_status.lower() == 'applied':
            update_range = f'Sheet1!D{row_index}:E{row_index}'
            body = {'values': [[new_status, today]]}
        elif new_status.lower() == 'did not proceed':
            update_range = f'Sheet1!D{row_index}:E{row_index}'
            body = {'values': [[new_status, ""]]}
        else:
            update_range = f'Sheet1!D{row_index}'
            body = {'values': [[new_status]]}

        service.spreadsheets().values().update(
            spreadsheetId=SPREADSHEET_ID,
            range=update_range,
            valueInputOption='USER_ENTERED',
            body=body
        ).execute()
        
        print(f"[{company}] '{job_title}' status updated to {new_status}!")
        return True
    except Exception as e:
        print(f"Error updating status in spreadsheet: {e}")
        return False

def log_applications_batch(rows):
    """Appends multiple rows to the Job Hunting spreadsheet in a single batch."""
    try:
        service = get_sheets_service()
        
        values = []
        for row in rows:
            company = row.get('company', '')
            job_title = row.get('job_title', '')
            tech_stack = row.get('tech_stack', '')
            status = row.get('status', 'Applied')
            date_applied = row.get('date_applied', '')
            job_link = row.get('job_link', '')
            location = row.get('location', '')
            salary = row.get('salary', '')
            contact = row.get('contact', '')
            
            # Columns: Company Name, Role Title, Tech Stack, Status, Application Date, Job Post Link, Contact Person, Salary / Package, Location
            values.append([company, job_title, tech_stack, status, date_applied, job_link, contact, salary, location])
            
        if not values:
            return True
            
        body = {
            'values': values
        }
        
        result = service.spreadsheets().values().append(
            spreadsheetId=SPREADSHEET_ID,
            range=RANGE_NAME,
            valueInputOption='USER_ENTERED',
            insertDataOption='INSERT_ROWS',
            body=body
        ).execute()

        print(f"Successfully logged {len(values)} applications to spreadsheet!")
        return True
    
    except Exception as e:
        print(f"Error batch logging to spreadsheet: {e}")
        return False

def get_recent_logs(limit=10):
    """Fetches the most recent logs from the Job Hunting spreadsheet."""
    try:
        service = get_sheets_service()
        result = service.spreadsheets().values().get(
            spreadsheetId=SPREADSHEET_ID,
            range=RANGE_NAME
        ).execute()
        
        values = result.get('values', [])
        if not values:
            return []
            
        recent = values[::-1]
        
        logs = []
        for row in recent[:limit]:
            while len(row) < 9:
                row.append('')
            # Skip header if it exists
            if row[0].lower() == 'company name' or row[1].lower() == 'role title':
                continue
            logs.append({
                'company': row[0],
                'job_title': row[1],
                'tech_stack': row[2],
                'status': row[3],
                'date_applied': row[4],
                'job_link': row[5],
            })
            
        return logs
    except Exception as e:
        print(f"Error fetching logs from spreadsheet: {e}")
        err_str = str(e).lower()
        if "credentials.json" in err_str or "invalid_grant" in err_str or "invalid jwt" in err_str:
            return {"auth_error": True}
        return []

def get_applied_job_ids():
    """Fetches all applied job IDs and signatures from the spreadsheet to prevent duplicates."""
    try:
        service = get_sheets_service()
        result = service.spreadsheets().values().get(
            spreadsheetId=SPREADSHEET_ID,
            range=RANGE_NAME
        ).execute()
        
        values = result.get('values', [])
        applied_jobs = {'ids': set(), 'signatures': set()}
        for row in values:
            if len(row) > 1:
                company = row[0].strip().lower()
                title = row[1].strip().lower()
                if company and title:
                    applied_jobs['signatures'].add(f"{company}|{title}")
                    
            if len(row) > 5 and row[5].strip():
                link = row[5].strip().split('?')[0]
                match = re.search(r'(\d{9,10})/?$', link)
                if match:
                    applied_jobs['ids'].add(match.group(1))
        return applied_jobs
    except Exception as e:
        print(f"Error fetching applied job IDs: {e}")
        return {'ids': set(), 'signatures': set()}

if __name__ == '__main__':
    # A simple test function to run when you execute `python tracker.py`
    print("Testing Google Sheets Integration...")
    log_application("Software Engineer", "Tech Corp", "2026-06-24", "Applied", "https://linkedin.com/jobs/123")
