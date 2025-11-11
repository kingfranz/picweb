var msg = document.getElementById("message");
var button = document.getElementById("button");
var textBox = document.getElementById("current-page");
var numThumbs = Number(document.getElementById("numOfThumbs").value);

// This event is fired when button is clicked
button.addEventListener("click", function ()
  {
   var vv = textBox.value;
   if (/^[0-9]+$/.test(vv) == false) {
       return;
   }
   var nn = Number(vv);
   if (nn < 500) {
        location.href = "/offset/" + (nn * numThumbs);
       return;
   }
   if (nn < 40000) {
         location.href = "/offset/" + nn;
         return;
   }
   var str = "/finddate/" + textBox.value;
   console.log(str);
   //msg.innerHTML += "< p > /offset/" + str + "</ >";
   location.href = str;
   });

textBox.addEventListener("keyup", function (event)
   {
    // Checking if key pressed is ENTER or not
    // if the key pressed is ENTER
    // click listener on button is called
    if (event.keyCode == 13) { button.click(); }
});
