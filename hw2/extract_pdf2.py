import pdfplumber

pdf_path = r"c:\Users\Mara\Desktop\seweb\hw2\Semantic Web Homework 2.pdf"
with pdfplumber.open(pdf_path) as pdf:
    for page in pdf.pages:
        print(page.extract_text())
