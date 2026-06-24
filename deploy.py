import subprocess
import os

def run_command(command, stdin_input=None):
    print(f"Running: {' '.join(command)}")
    proc = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, shell=True)
    out, err = proc.communicate(input=stdin_input)
    print("OUT:", out)
    if err:
        print("ERR:", err)
    return out

print("1. Creating and linking the Vercel project...")
run_command(["npx", "vercel", "link", "--yes"])
run_command(["npx", "vercel", "--yes"])

print("2. Adding Environment Variables...")

# Add GEMINI_API_KEY and EMAIL_PASSWORD from .env
if os.path.exists('.env'):
    with open('.env', 'r') as f:
        for line in f:
            if '=' in line:
                key, value = line.strip().split('=', 1)
                print(f"Updating {key}...")
                run_command(["npx", "vercel", "env", "rm", key, "production", "--yes"])
                run_command(["npx", "vercel", "env", "add", key, "production"], stdin_input=value + "\n")

# Add GOOGLE_CREDENTIALS from credentials.json
if os.path.exists('credentials.json'):
    with open('credentials.json', 'r') as f:
        creds = f.read()
        print("Updating GOOGLE_CREDENTIALS...")
        run_command(["npx", "vercel", "env", "rm", "GOOGLE_CREDENTIALS", "production", "--yes"])
        run_command(["npx", "vercel", "env", "add", "GOOGLE_CREDENTIALS", "production"], stdin_input=creds + "\n")

print("3. Deploying to Production...")
out = run_command(["npx", "vercel", "--prod", "--yes"])

print("Deployment Complete! Look for your production URL in the output above.")
