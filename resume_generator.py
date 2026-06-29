import os
import json
import io
import re
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

# Common tech/business keywords for heuristic matching
COMMON_KEYWORDS = {
    "python", "java", "javascript", "react", "node", "aws", "azure", "gcp", "sql", "nosql", 
    "docker", "kubernetes", "agile", "scrum", "leadership", "communication", "machine learning",
    "ai", "data pipelines", "llm", "automation", "debugging", "healthcare", "client collaboration",
    "lean six sigma", "frontend", "backend", "fullstack", "devops", "ci/cd", "rest", "graphql",
    "typescript", "c++", "c#", "go", "ruby", "php", "django", "flask", "spring", "vue", "angular",
    "git", "linux", "algorithms", "data structures", "system design"
}

def extract_keywords(job_description, profile_skills):
    """Heuristic keyword extraction for missing and included keywords."""
    jd_lower = job_description.lower()
    
    # Simple word boundary check for each keyword in our common list
    jd_keywords = set()
    for kw in COMMON_KEYWORDS:
        if re.search(r'\b' + re.escape(kw) + r'\b', jd_lower):
            jd_keywords.add(kw.title() if len(kw) > 3 else kw.upper())
            
    # Also extract words that are Capitalized in the JD (cheap heuristic for tools/frameworks)
    # E.g. "React", "Docker" - skip start of sentences heuristically if needed, but simple is fine.
    capitalized = re.findall(r'\b[A-Z][a-zA-Z]{2,}\b', job_description)
    for cap in capitalized:
        if cap.lower() in COMMON_KEYWORDS:
            jd_keywords.add(cap)
            
    # Normalize profile skills
    profile_skills_lower = {s.lower(): s for s in profile_skills}
    
    keywords_to_include = list(jd_keywords)
    missing_keywords = []
    
    for kw in keywords_to_include:
        if kw.lower() not in profile_skills_lower:
            missing_keywords.append(kw)
            
    # Also find which profile skills match the JD
    matched_profile_skills = []
    for skill_lower, skill_original in profile_skills_lower.items():
        if re.search(r'\b' + re.escape(skill_lower) + r'\b', jd_lower):
            matched_profile_skills.append(skill_original)
            
    return sorted(keywords_to_include), sorted(missing_keywords), sorted(matched_profile_skills)


def get_tailored_profile_data(master_profile, job_description):
    """Uses NLP matching to tailor the master profile to the job description."""
    jd_lower = job_description.lower()
    
    # 1. Map personal info
    p_info = master_profile.get("personal_info", {})
    first_name = p_info.get("first_name", "")
    last_name = p_info.get("last_name", "")
    
    # Prioritize portfolio over linkedin if both exist
    link = p_info.get("portfolio_url", "")
    if not link:
        link = p_info.get("linkedin_url", "")
        
    tailored_data = {
        "personal_info": {
            "name": f"{first_name} {last_name}".strip(),
            "email": p_info.get("email", ""),
            "phone": p_info.get("phone", ""),
            "location": p_info.get("location", ""),
            "link": link
        },
        "summary": master_profile.get("summary", ""),
        "sections": []
    }
    
    # 2. Map Experience
    exp_items = []
    for exp in master_profile.get("experience", []):
        # Split description into bullets by period
        desc = exp.get("description", "")
        bullets = [b.strip() + "." for b in desc.split(". ") if b.strip()]
        
        start = exp.get("start_date", "")
        end = exp.get("end_date", "")
        date_str = f"{start} - {end}" if start and end else (start or end)
        
        company = exp.get("company", "")
        location = exp.get("location", "")
        subtitle = f"{company} | {location}" if location else company
        
        exp_items.append({
            "title": exp.get("title", ""),
            "subtitle": subtitle,
            "date": date_str,
            "bullets": bullets
        })
        
    if exp_items:
        tailored_data["sections"].append({
            "title": "WORK EXPERIENCE",
            "items": exp_items
        })
        
    # 3. Map Education
    edu_items = []
    for edu in master_profile.get("education", []):
        edu_items.append({
            "title": edu.get("degree", ""),
            "subtitle": edu.get("university", ""),
            "date": edu.get("graduation_year", ""),
            "bullets": []
        })
        
    if edu_items:
        tailored_data["sections"].append({
            "title": "EDUCATION",
            "items": edu_items
        })
        
    import html
    
    # 4. Map Projects
    proj_items = []
    for proj in master_profile.get("projects", []):
        desc = proj.get("description", "")
        # Split description into bullets by period space
        bullets = [b.strip() + "." for b in desc.split(". ") if b.strip()]
        # Cleanup double periods if any
        bullets = [b[:-1] if b.endswith("..") else b for b in bullets]
        
        role = proj.get("role", "")
        link = proj.get("link", "")
        
        if link:
            # ReportLab parses text as XML, so we must escape characters like '&'
            safe_link = html.escape(link)
            subtitle = f"{role} | <a href='{safe_link}'>{safe_link}</a>"
        else:
            subtitle = role
        
        proj_items.append({
            "title": proj.get("title", ""),
            "subtitle": subtitle,
            "date": "",
            "bullets": bullets
        })
        
    if proj_items:
        tailored_data["sections"].append({
            "title": "PROJECTS",
            "items": proj_items
        })

    # 5. Certifications, Awards, and Skills
    all_skills = master_profile.get("skills", [])
    matched_skills = []
    other_skills = []
    
    for skill in all_skills:
        # Check if skill exists as a whole word in JD
        skill_lower = skill.lower()
        if re.search(r'\b' + re.escape(skill_lower) + r'\b', jd_lower):
            matched_skills.append(skill)
        else:
            other_skills.append(skill)
            
    # Combine matched skills first, then pad with other skills up to a reasonable amount (e.g. 15 skills)
    final_skills = matched_skills + other_skills
    final_skills = final_skills[:15] # Don't overwhelm the resume
    
    certs = master_profile.get("certifications", [])
    awards = master_profile.get("awards", [])
    
    certs_awards_skills_items = []
    
    if certs:
        certs_awards_skills_items.append({
            "title": "Certifications",
            "subtitle": "",
            "date": "",
            "bullets": [", ".join(certs)]
        })
        
    if final_skills:
        skills_str = ", ".join(final_skills)
        certs_awards_skills_items.append({
            "title": "Technical Skills",
            "subtitle": "",
            "date": "",
            "bullets": [skills_str]
        })
        
    if awards:
        certs_awards_skills_items.append({
            "title": "Awards & Achievements",
            "subtitle": "",
            "date": "",
            "bullets": [", ".join(awards)]
        })
        
    if certs_awards_skills_items:
        tailored_data["sections"].append({
            "title": "CERTIFICATIONS, SKILLS & AWARDS",
            "items": certs_awards_skills_items
        })
        
    return tailored_data

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
