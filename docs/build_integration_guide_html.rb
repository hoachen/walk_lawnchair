require "cgi"

source = File.join(__dir__, "Launcher_Tool_App_Integration_Guide.md")
output = File.join(__dir__, "Launcher_Tool_App_Integration_Guide.html")

def inline(text)
  escaped = CGI.escapeHTML(text)
  escaped.gsub!(/`([^`]+)`/) { "<code>#{$1}</code>" }
  escaped.gsub!(/\*\*([^*]+)\*\*/) { "<strong>#{$1}</strong>" }
  escaped
end

lines = File.readlines(source, encoding: "UTF-8", chomp: true)
body = []
index = 0
in_list = false

close_list = lambda do
  if in_list
    body << "</ul>"
    in_list = false
  end
end

while index < lines.length
  line = lines[index]
  if line.start_with?("# ")
    index += 1
    next
  elsif line.start_with?("```")
    close_list.call
    code = []
    index += 1
    while index < lines.length && !lines[index].start_with?("```")
      code << lines[index]
      index += 1
    end
    body << "<pre><code>#{CGI.escapeHTML(code.join("\n"))}</code></pre>"
  elsif line.match?(/^\|.*\|$/)
    close_list.call
    rows = []
    while index < lines.length && lines[index].match?(/^\|.*\|$/)
      row = lines[index]
      unless row.match?(/^\|\s*[-:]+/)
        rows << row.split("|")[1..-2].map(&:strip)
      end
      index += 1
    end
    html = ["<table>"]
    rows.each_with_index do |cells, row_index|
      tag = row_index.zero? ? "th" : "td"
      html << "<tr>#{cells.map { |cell| "<#{tag}>#{inline(cell)}</#{tag}>" }.join}</tr>"
    end
    html << "</table>"
    body << html.join
    index -= 1
  elsif line.start_with?("## ")
    close_list.call
    body << "<h1>#{inline(line[3..])}</h1>"
  elsif line.start_with?("### ")
    close_list.call
    body << "<h2>#{inline(line[4..])}</h2>"
  elsif line.start_with?("> ")
    close_list.call
    body << "<blockquote>#{inline(line[2..])}</blockquote>"
  elsif line.match?(/^[-*] /)
    unless in_list
      body << "<ul>"
      in_list = true
    end
    body << "<li>#{inline(line[2..])}</li>"
  elsif line.match?(/^\d+\. /)
    close_list.call
    body << "<p class=\"step\">#{inline(line)}</p>"
  elsif line.strip.empty?
    close_list.call
  else
    close_list.call
    body << "<p>#{inline(line)}</p>"
  end
  index += 1
end
close_list.call

html = <<~HTML
  <!doctype html><html><head><meta charset="utf-8"><style>
  @page { margin: 0.75in 0.8in; }
  body { font-family: Arial, sans-serif; font-size: 10.5pt; line-height: 1.3; color: #1f2933; }
  .title { font-size: 22pt; font-weight: bold; color: #0b2545; margin: 0 0 4pt; }
  .meta { color: #595959; margin: 0 0 18pt; }
  h1 { color: #2e74b5; font-size: 16pt; margin: 17pt 0 7pt; page-break-after: avoid; }
  h2 { color: #1f4e78; font-size: 12pt; margin: 12pt 0 5pt; page-break-after: avoid; }
  p { margin: 0 0 6pt; } ul { margin: 0 0 7pt 18pt; padding: 0; } li { margin: 0 0 3pt; }
  code { font-family: Menlo, monospace; color: #1f4e78; } pre { background: #f2f4f7; padding: 8pt; white-space: pre-wrap; font-size: 8.5pt; }
  blockquote { background: #f4f6f9; border-left: 3pt solid #2e74b5; margin: 8pt 0; padding: 7pt 9pt; }
  table { border-collapse: collapse; width: 100%; margin: 7pt 0 10pt; } th { background: #e8eef5; } th, td { border: 1px solid #b8c4d0; padding: 5pt; vertical-align: top; text-align: left; font-size: 9pt; }
  .step { margin-left: 10pt; } footer { color: #595959; font-size: 8.5pt; text-align: right; margin-top: 12pt; }
  </style></head><body><p class="title">Walk Lawnchair：工具 App 接入与页面定制指南</p><p class="meta">适用发布物：launcher-sdk 1.0.15　|　最后更新：2026-08-01</p>#{body.join("\n")}<footer>Walk Lawnchair 集成文档</footer></body></html>
HTML

File.write(output, html, mode: "w", encoding: "UTF-8")
