param(
    [string]$Adb = 'C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe',
    [string]$Serial = '127.0.0.1:16384'
)
$ErrorActionPreference = 'Stop'
# Visible UI only. Do not run on private conversations, credential forms or administrator logs.
$path = '/data/local/tmp/novalpie-beta7-ui-' + [Guid]::NewGuid().ToString('N') + '.xml'
$capture = & $Adb -s $Serial shell uiautomator dump $path 2>&1
if (($capture -join ' ') -notlike "*dumped to: $path*") { throw "Could not capture active UI: $($capture -join ' ')" }
$raw = & $Adb -s $Serial shell cat $path
[xml]$tree = $raw -join "`n"
if ($tree.DocumentElement.Name -ne 'hierarchy') { throw 'Scoped capture is not a UI hierarchy' }
& $Adb -s $Serial shell rm $path | Out-Null
$rows = foreach ($node in $tree.SelectNodes('//node')) {
    $text = [string]$node.text; $description = [string]$node.'content-desc'
    if (!$text -and !$description) { continue }
    if ($text -match '(?i)bearer\s|auth_token|[A-Za-z0-9_.+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}' -or $node.password -eq 'true') { continue }
    [pscustomobject]@{text=$text;description=$description;bounds=[string]$node.bounds;clickable=[string]$node.clickable;enabled=[string]$node.enabled}
}
$rows | ConvertTo-Json -Depth 4 -Compress
