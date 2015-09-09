<?php

// Create connection
$conn = new mysqli("localhost", "root", "admin", "measurements");
// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$sql = "SELECT ttime, temperature, humidity FROM measurements WHERE ttime > NOW() - INTERVAL 3 DAY;";
$result = $conn->query($sql);

?>

<html>
<head>
<!-- Load c3.css -->
<link href="https://rawgit.com/masayuki0812/c3/master/c3.min.css" rel="stylesheet" type="text/css">

<!-- Load d3.js and c3.js -->
<script src="https://rawgit.com/mbostock/d3/master/d3.min.js" charset="utf-8"></script>
<script src="https://rawgit.com/masayuki0812/c3/master/c3.min.js"></script>
</head>
<body>
<div id="chart"></div>

<script>

<?php

if($result->num_rows > 0) {
?>
var json = [
<?php
  while($row = $result->fetch_assoc()) {
    ?>{ttime:'<?=$row["ttime"]?>',temperature:<?=$row["temperature"]?>,humidity:<?=$row["humidity"]?>},<?
  }
}
?>
];
<?php
$conn->close();
?>
var chart = c3.generate({
    bindto: '#chart',
    data: {
      x: 'ttime',
      xFormat: '%Y-%m-%d %H:%M:%S', 
      keys: {
        x:'ttime',
        value: ['temperature', 'humidity']
      },
      json: json,
      axes: {
        temperature: 'y',
        humidity: 'y2'
      }
    },
    axis: {
        x: {
            type: 'timeseries',
            tick: {
                format: '%Y-%m-%d %H:%M'
            }
        },
        y: {
            label: 'temperature'
        },
        y2: {
            show: true,
            label: 'humidity'
        }
    }
});
</script>
</body>
</html>


