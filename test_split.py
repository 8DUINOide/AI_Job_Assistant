import json
from resume_generator import convert_profile_to_pdf_data

with open('master_profile.json', 'r') as f:
    master_profile = json.load(f)

pdf_data = convert_profile_to_pdf_data(master_profile)

for section in pdf_data['sections']:
    if section['title'] == 'PROJECTS':
        for item in section['items']:
            if 'JobAi' in item['title']:
                print("JobAi Project Bullets:")
                for idx, bullet in enumerate(item['bullets']):
                    print(f"{idx+1}: {bullet}")
