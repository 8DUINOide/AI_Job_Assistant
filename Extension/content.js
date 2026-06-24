chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === "fill_form") {
    
    const inputs = Array.from(document.querySelectorAll('input:not([type="hidden"]), textarea, select'));
    const formFields = inputs.map(input => {
      let label = "";
      if (input.id) {
          const labelElem = document.querySelector(`label[for="${input.id}"]`);
          if (labelElem) label = labelElem.innerText;
      }
      // Often in React apps, the label is a parent or preceding sibling
      if (!label && input.parentElement && input.parentElement.innerText) {
          label = input.parentElement.innerText.split('\n')[0];
      }
      if (!label && input.name) label = input.name;
      if (!label && input.placeholder) label = input.placeholder;
      
      return {
          id: input.id,
          name: input.name,
          type: input.type,
          label: label ? label.trim() : "Unknown Field"
      };
    });

    console.log("Fields sent to AI:", formFields);

    fetch('http://localhost:5000/api/fill-form', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fields: formFields })
    })
    .then(response => response.json())
    .then(data => {
        const answers = data.answers;
        console.log("Answers received from AI:", answers);
        
        inputs.forEach(input => {
            const key = input.id || input.name;
            if (answers[key]) {
                // React bypass: modern frameworks hijack the native value setter
                const prototype = Object.getPrototypeOf(input);
                const setter = Object.getOwnPropertyDescriptor(prototype, 'value')?.set;
                
                if (setter) {
                    setter.call(input, answers[key]);
                } else {
                    input.value = answers[key];
                }
                
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
            }
        });
        
        sendResponse({ success: true });
    })
    .catch(error => {
        console.error("Error asking AI to fill form:", error);
        sendResponse({ success: false });
    });
    
    return true; 
  }
});
