import urllib.request
import os

BOUNDARY = '----WebKitFormBoundary7MA4YWxkTrZu0gW'
file_path = 'resume.pdf'
with open(file_path, 'rb') as f:
    file_content = f.read()

body = (
    b'--' + BOUNDARY.encode('utf-8') + b'\r\n'
    b'Content-Disposition: form-data; name="file"; filename="resume.pdf"\r\n'
    b'Content-Type: application/pdf\r\n\r\n' +
    file_content + b'\r\n' +
    b'--' + BOUNDARY.encode('utf-8') + b'--\r\n'
)

req = urllib.request.Request(
    'https://ai-job-assistant-one.vercel.app/api/build-profile',
    data=body,
    headers={
        'Content-Type': 'multipart/form-data; boundary=' + BOUNDARY,
        'Authorization': 'Bearer secret_admin_token_123_abc_xyz'
    }
)
try:
    response = urllib.request.urlopen(req)
    print('Success:', response.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print('HTTPError:', e.code)
    print(e.read().decode('utf-8'))
