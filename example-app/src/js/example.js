import { custominappbrowser } from 'custominappbrowser';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    custominappbrowser.echo({ value: inputValue })
}
