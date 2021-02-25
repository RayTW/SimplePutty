# SimplePutty
簡易管理ssh ip、user工具，僅適用於mac os。 
Easy management of "putty.json" file for Mac OS.

## Tutorial
### 使用方法1 use case 1
#### 啟動應用 Lanunch applaction
step1 java -jar SimplePutty.jar 或 double click SimplePutty.jar
step2 允許讀檔權限 Enable permission
![](https://github.com/RayTW/SimplePutty/blob/main/OpenpPrmission.png?raw=true)

### 使用方法2 use case 2
#### 從windows putty應用程式轉來使用此app
1.用windows的putty匯出"putty.xml"檔案，並用終端機執行指令"java -jar SimplePutty.jar putty.xml"。
2.將在控制台輸出的json格式字串copy存為"putty.json。

#### Migration from windows putty.
1.Export file "putty.xml" uses windows application putty, and execute command line "java -jar SimplePutty.jar putty.xml" on terminal.
2.Copy the json output save as file "putty.json".

#preview
![](https://github.com/RayTW/SimplePutty/blob/main/SimplePuttyPreview.png?raw=true)
