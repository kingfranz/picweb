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
   var nn = Number(vv);
   if (nn < 500) {
      var str = "/" + owner.value + "?offset=" + (nn * numThumbs);
      console.log(str);
      location.href = str;
      return;
   }
   if (nn < 40000) {
      var str = "/findimg?target=" + nn;
      console.log(str);
      location.href = str;
      return;
   }
   var str = "/finddate?target=" + textBox.value;
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
