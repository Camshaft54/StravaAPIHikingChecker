let url = window.location.href;
let urlList = url.split("?state=&code=");
if (urlList.length === 2) {
    let authCode = urlList[1].substring(0, urlList[1].indexOf("&scope="));
    console.log(authCode);
    window.location.href = "success.html?" + authCode;
    document.getElementById("code").value = authCode;
} else {
    alert("Unable to process request, please contact the administrator")
}