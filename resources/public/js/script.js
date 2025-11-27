var msg = document.getElementById("message");
var button = document.getElementById("button");
var textBox = document.getElementById("current-page");
var numThumbs = Number(document.getElementById("numOfThumbs").value);
var owner = document.getElementById("owner");

// This event is fired when button is clicked
button.addEventListener("click", function ()
  {
   var vv = textBox.value;
   if (/^[0-9]+$/.test(vv) == false) {
       return;
   }
   var str = "/contact?start=" + textBox.value;
   console.log(str);
   location.href = str;
   });

textBox.addEventListener("keyup", function (event)
   {
    // Checking if key pressed is ENTER or not
    // if the key pressed is ENTER
    // click listener on button is called
    if (event.keyCode == 13) { button.click(); }
});
