import os
import json
from google.oauth2 import service_account
from googleapiclient.discovery import build

# If modifying these scopes, delete the file token.json.
SCOPES = ['https://www.googleapis.com/auth/spreadsheets']

# This is a placeholder. We will need the actual Spreadsheet ID from your "Job Hunting" sheet's URL.
# Example: https://docs.google.com/spreadsheets/d/YOUR_SPREADSHEET_ID/edit
SPREADSHEET_ID = '1-9st_OvzdkFf8qFAek5fCKnm1ta1UqRXpejAXW0IJdY'
RANGE_NAME = 'Sheet1!A:E'  # Adjust based on your sheet name and columns

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
        
        # The data to insert
        values = [
            [job_title, company, date_applied, status, job_link]
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

if __name__ == '__main__':
    # A simple test function to run when you execute `python tracker.py`
    print("Testing Google Sheets Integration...")
    log_application("Software Engineer", "Tech Corp", "2026-06-24", "Applied", "https://linkedin.com/jobs/123")
