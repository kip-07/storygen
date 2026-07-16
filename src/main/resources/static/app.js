document.getElementById('storyForm').addEventListener('submit', async function (e) {
    e.preventDefault();

    const prompt = document.getElementById('prompt').value.trim();
    const errorDiv = document.getElementById('error');
    const resultDiv = document.getElementById('result');
    const preview = document.getElementById('preview');
    const downloadLink = document.getElementById('downloadLink');
    const generateBtn = document.getElementById('generateBtn');

    errorDiv.style.display = 'none';
    resultDiv.style.display = 'none';
    generateBtn.disabled = true;
    generateBtn.textContent = 'Generating...';

    const body = { prompt: prompt };

    try {
        const response = await fetch('/api/stories', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        const data = await response.json();

        if (!response.ok) {
            errorDiv.textContent = data.error || 'An error occurred';
            errorDiv.style.display = 'block';
            return;
        }

        preview.srcdoc = data.htmlContent;
        downloadLink.href = data.downloadUrl;
        downloadLink.style.display = 'inline-block';
        resultDiv.style.display = 'block';
    } catch (err) {
        errorDiv.textContent = 'Network error: ' + err.message;
        errorDiv.style.display = 'block';
    } finally {
        generateBtn.disabled = false;
        generateBtn.textContent = 'Generate Story';
    }
});
