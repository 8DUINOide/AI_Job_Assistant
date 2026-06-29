import os
import json
import io
import google.generativeai as genai
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, HRFlowable, ListFlowable, ListItem
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib import colors
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase import pdfmetrics

# Try to use standard fonts, Helvetica is built-in
FONT_NAME = "Helvetica"
FONT_BOLD = "Helvetica-Bold"
FONT_OBLIQUE = "Helvetica-Oblique"

def get_tailored_profile_data(master_profile, job_description):
    """Uses Gemini to tailor the master profile to the job description."""
    genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
    model = genai.GenerativeModel('gemini-2.5-flash')
    
    prompt = f"""
    You are an expert tech recruiter and resume writer. 
    Your goal is to tailor the candidate's Master Profile to the Target Job Description.
    
    CRITICAL RULE: DO NOT INVENT OR HALLUCINATE ANY FACTS, EXPERIENCE, SKILLS, OR PROJECTS that are not present in the Master Profile. You may only select, reorder, and rewrite existing facts to highlight their relevance to the Job Description.

    Target Job Description:
    {job_description}
    
    Candidate's Master Profile (JSON):
    {json.dumps(master_profile)}
    
    Provide the tailored resume data as a JSON object with the following schema:
    {{
        "personal_info": {{
            "name": "First Last",
            "email": "email@example.com",
            "phone": "Phone number",
            "location": "Location",
            "link": "github/portfolio url"
        }},
        "summary": "A strong, tailored professional summary.",
        "sections": [
            {{
                "title": "WORK EXPERIENCE",
                "items": [
                    {{
                        "title": "Job Title",
                        "subtitle": "Company",
                        "date": "Start - End",
                        "bullets": ["Bullet 1", "Bullet 2"]
                    }}
                ]
            }},
            {{
                "title": "EDUCATION",
                "items": [
                    {{
                        "title": "Degree",
                        "subtitle": "University",
                        "date": "Graduation Year",
                        "bullets": []
                    }}
                ]
            }},
            {{
                "title": "SKILLS",
                "items": [
                    {{
                        "title": "Technical Skills",
                        "subtitle": "",
                        "date": "",
                        "bullets": ["Skill 1, Skill 2, Skill 3"]
                    }}
                ]
            }}
        ]
    }}
    
    Feel free to create a "PROJECTS" section if the master profile implies projects in the experience or skills, but again, only if the data is implicitly or explicitly there.
    """
    
    # We do NOT swallow exceptions here so that the Flask API can catch them and return the exact error message to the frontend.
    response = model.generate_content(prompt, generation_config={"response_mime_type": "application/json"})
    text = response.text.strip()
    return json.loads(text)

def generate_pdf_from_data(data):
    """Generates a PDF using reportlab based on the tailored data."""
    buffer = io.BytesIO()
    
    # Setup styles
    styles = getSampleStyleSheet()
    
    # Custom styles
    name_style = ParagraphStyle(
        'NameStyle',
        parent=styles['Heading1'],
        fontName=FONT_BOLD,
        fontSize=18,
        spaceAfter=4,
        alignment=TA_CENTER
    )
    
    contact_style = ParagraphStyle(
        'ContactStyle',
        parent=styles['Normal'],
        fontName=FONT_NAME,
        fontSize=9,
        spaceAfter=10,
        alignment=TA_CENTER
    )
    
    section_title_style = ParagraphStyle(
        'SectionTitleStyle',
        parent=styles['Heading2'],
        fontName=FONT_BOLD,
        fontSize=11,
        spaceBefore=10,
        spaceAfter=4,
        textTransform='uppercase'
    )
    
    summary_style = ParagraphStyle(
        'SummaryStyle',
        parent=styles['Normal'],
        fontName=FONT_NAME,
        fontSize=10,
        leading=14,
        spaceAfter=10,
        alignment=TA_JUSTIFY
    )
    
    item_title_style = ParagraphStyle(
        'ItemTitleStyle',
        parent=styles['Normal'],
        fontName=FONT_BOLD,
        fontSize=10,
        spaceBefore=6,
        spaceAfter=2
    )
    
    item_subtitle_style = ParagraphStyle(
        'ItemSubtitleStyle',
        parent=styles['Normal'],
        fontName=FONT_OBLIQUE,
        fontSize=10,
        spaceAfter=4
    )
    
    bullet_style = ParagraphStyle(
        'BulletStyle',
        parent=styles['Normal'],
        fontName=FONT_NAME,
        fontSize=10,
        leading=14,
        leftIndent=15,
        firstLineIndent=0,
        spaceAfter=3,
        alignment=TA_JUSTIFY
    )

    doc = SimpleDocTemplate(
        buffer,
        pagesize=letter,
        rightMargin=36,
        leftMargin=36,
        topMargin=36,
        bottomMargin=36
    )
    
    story = []
    
    p_info = data.get('personal_info', {})
    name = p_info.get('name', 'Name')
    email = p_info.get('email', '')
    phone = p_info.get('phone', '')
    location = p_info.get('location', '')
    link = p_info.get('link', '')
    
    # Render Name
    story.append(Paragraph(name.upper(), name_style))
    
    # Render Contact Info separated by diamond (&#9830;) or bullet
    contact_parts = [p for p in [email, phone, location, link] if p]
    contact_str = " &nbsp;&#9830;&nbsp; ".join(contact_parts)
    story.append(Paragraph(contact_str, contact_style))
    
    # Horizontal line
    story.append(HRFlowable(width="100%", thickness=1, color=colors.black, spaceBefore=0, spaceAfter=8))
    
    # Summary
    if data.get('summary'):
        story.append(Paragraph("PROFESSIONAL SUMMARY", section_title_style))
        story.append(HRFlowable(width="100%", thickness=1, color=colors.black, spaceBefore=0, spaceAfter=6))
        story.append(Paragraph(data['summary'], summary_style))
    
    # Sections
    for section in data.get('sections', []):
        story.append(Paragraph(section.get('title', 'SECTION'), section_title_style))
        story.append(HRFlowable(width="100%", thickness=1, color=colors.black, spaceBefore=0, spaceAfter=6))
        
        for item in section.get('items', []):
            # We want Title on left, Date on right
            # We can use a table or floating text. A simple table works best for left/right alignment.
            from reportlab.platypus import Table, TableStyle
            
            title_p = Paragraph(item.get('title', ''), item_title_style)
            date_p = Paragraph(f"<font name='{FONT_BOLD}'>{item.get('date', '')}</font>", ParagraphStyle('R', alignment=2, fontName=FONT_BOLD, fontSize=10))
            
            t1 = Table([[title_p, date_p]], colWidths=['75%', '25%'])
            t1.setStyle(TableStyle([
                ('LEFTPADDING', (0,0), (-1,-1), 0),
                ('RIGHTPADDING', (0,0), (-1,-1), 0),
                ('TOPPADDING', (0,0), (-1,-1), 0),
                ('BOTTOMPADDING', (0,0), (-1,-1), 0),
                ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ]))
            story.append(t1)
            
            if item.get('subtitle'):
                subtitle_p = Paragraph(item.get('subtitle', ''), item_subtitle_style)
                story.append(subtitle_p)
                
            bullets = item.get('bullets', [])
            if bullets:
                bullet_data = []
                for b in bullets:
                    bullet_data.append(ListItem(Paragraph(b, bullet_style), leftIndent=15, bulletColor=colors.black))
                story.append(ListFlowable(bullet_data, bulletType='bullet', start='bulletchar'))
    
    doc.build(story)
    buffer.seek(0)
    return buffer
