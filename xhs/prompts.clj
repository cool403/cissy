(ns xhs.prompts)

;; get post prompt
(defn get-post-promt [^String text]
  (str "" "
请从以下文本中提取以下要素：
1. 帖子链接
2. 发帖人
3. 发帖人首页链接
4. 帖子标题
          " ""
       \newline
       (str "文本:" text)
       \newline
       "" "
请返回一个纯 JSON 格式的数据，不要包含任何额外的标记或注释。例如:
[{
  \"post_url\": \"\",
  \"uid\": \"\",
  \"page_url\": \"\",
  \"title\": \"\"
}]
          " ""))