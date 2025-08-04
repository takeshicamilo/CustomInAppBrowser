import { custominappbrowser } from 'custominappbrowser';

window.testEcho = async () => {
    const inputValue = document.getElementById("echoInput").value;
    try {
        const result = await custominappbrowser.echo({ value: inputValue });
        document.getElementById("result").innerHTML = `<strong>Echo Result:</strong> ${result.value}`;
    } catch (error) {
        document.getElementById("result").innerHTML = `<strong>Echo Error:</strong> ${error}`;
    }
}

window.openFullscreenWebView = async () => {
    const url = document.getElementById("urlInput").value;
    
    if (!url) {
        document.getElementById("result").innerHTML = "<strong>Error:</strong> Please enter a URL";
        return;
    }
    
    // Ensure URL has protocol
    let formattedUrl = url;
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        formattedUrl = 'https://' + url;
    }
    
    try {
        document.getElementById("result").innerHTML = "<strong>Opening:</strong> " + formattedUrl;
        const result = await custominappbrowser.openUrl({ url: formattedUrl });
        if (result.success) {
            document.getElementById("result").innerHTML = `<strong>Success:</strong> Fullscreen WebView opened for ${formattedUrl}`;
        }
    } catch (error) {
        document.getElementById("result").innerHTML = `<strong>Error:</strong> ${error}`;
    }
}
