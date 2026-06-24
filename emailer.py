import smtplib
from email.message import EmailMessage
import os

def send_job_digest(target_email, high_match_jobs):
    """Sends an email with a list of high-matching jobs."""
    if not high_match_jobs:
        print("No high match jobs to email.")
        return

    # Use your own email to send the message to yourself
    sender_email = target_email 
    sender_password = os.getenv("EMAIL_PASSWORD")

    if not sender_password:
        print("WARNING: EMAIL_PASSWORD not found in .env file. Could not send email.")
        return

    msg = EmailMessage()
    msg['Subject'] = f"AI Job Assistant: Found {len(high_match_jobs)} High-Match Jobs!"
    msg['From'] = sender_email
    msg['To'] = target_email

    content = "Hello! Your AI Job Assistant found the following highly relevant jobs in the last 24 hours:\n\n"
    
    for job in high_match_jobs:
        content += f"Role: {job['title']} at {job['company']}\n"
        content += f"Match Score: {job['score']}/100\n"
        content += f"AI Reason: {job['reason']}\n"
        content += f"Apply Link: {job['link']}\n"
        content += "-" * 40 + "\n\n"

    content += "Good luck applying!\n- Your AI Assistant"
    msg.set_content(content)

    try:
        # Connect to Gmail's SMTP server
        with smtplib.SMTP_SSL('smtp.gmail.com', 465) as smtp:
            smtp.login(sender_email, sender_password)
            smtp.send_message(msg)
        print(f"Successfully sent email digest to {target_email}!")
    except Exception as e:
        print(f"Error sending email: {e}")

if __name__ == "__main__":
    # Test
    test_jobs = [{
        "title": "Junior Full Stack Developer",
        "company": "Test Company",
        "score": 95,
        "reason": "Perfect match based on internship.",
        "link": "https://linkedin.com"
    }]
    send_job_digest("alfrancisbadillapaz10@gmail.com", test_jobs)
