<!DOCTYPE html>
<html lang="ja">
<head>
	<meta charset="utf-8">
	<link rel="stylesheet" href="${baseurl}css/main.css">
	<title>${title!}</title>
</head>
<body>
	<div class="content markdown">
		<@markdown replace_backslash_to_yensign=true>${content!}</@markdown>
	</div>

	<#if (_PREVIEW!false) == true>
	<script type="text/javascript">
		function waitForUpdate() {
			var xhr = new XMLHttpRequest();
			xhr.onload = function (e) {
				if (xhr.readyState === 4) {
					if (xhr.status === 200) {
						location.reload();
					}
					waitForUpdate();
				}
			};
			xhr.onerror = function (e) {
				waitForUpdate();
			};
			xhr.open("GET", "/wait-for-update", true);
			xhr.send(null);
		}
		waitForUpdate();
	</script>
	</#if>

</body>
</html>
