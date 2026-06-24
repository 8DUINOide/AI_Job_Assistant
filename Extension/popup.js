document.getElementById('fillFormBtn').addEventListener('click', async () => {
  const status = document.getElementById('status');
  status.textContent = 'Reading form...';
  
  // Get active tab
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  
  // Tell content.js to scan and fill the form
  chrome.tabs.sendMessage(tab.id, { action: "fill_form" }, (response) => {
    if (chrome.runtime.lastError) {
      status.textContent = 'Error connecting to page. Refresh and try again.';
      return;
    }
    if (response && response.success) {
      status.textContent = 'Form successfully filled!';
    } else {
      status.textContent = 'Could not fill form. Is the backend running?';
    }
  });
});

document.getElementById('logJobBtn').addEventListener('click', async () => {
    const status = document.getElementById('status');
    status.textContent = 'Logging to spreadsheet...';
    
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    
    // We send a message to our local python backend to log it
    fetch('http://localhost:5000/api/log-job', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: tab.url, title: tab.title })
    }).then(res => res.json())
      .then(data => {
          status.textContent = 'Logged successfully!';
      }).catch(err => {
          status.textContent = 'Failed to log. Is the backend running?';
      });
});
