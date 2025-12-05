//var msg = document.getElementById("message");
var button = document.getElementById("button");
var textBox = document.getElementById("current-page");
//var numThumbs = Number(document.getElementById("numOfThumbs").value);
//var owner = document.getElementById("owner");

// This event is fired when button is clicked
button.addEventListener("click", function ()
  {
  // (?:19|20)\d{2}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d$
   var vv = textBox.value;
   //console.log(vv);
   if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(vv) == false) {
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
