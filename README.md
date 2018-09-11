# SpringBatch
Spring-Batch的測試與練習

## Overall

![spribatch](https://user-images.githubusercontent.com/24807021/45210259-a74be000-b2c1-11e8-86fa-0d3085812500.png)

## JobLauncher
- Job的啟動器，一定要塞JobRepository進去，目前放在排程器裡面使用。

## JobRepository

- 工作儲存庫，存放所有SpringBatch需要寫入的工作資訊，預設是使用hsqldb，且在記憶體內運行。
- 此版本改為Mysql (MariaDB)，原因是方便觀察數據的寫入。
- 要塞一個dataSource才可用
- 操作上沒有很方便，不如用JobExplorer

## JobExplorer

- JobRepository的簡易唯讀版,可以取得執行過的工作資訊。
- 要塞一個dataSource才可用
- 用起來比較安心，不會操作到資料。

## Job

- SpringBatch的工作單位，結構組成如上。
- 定義工作內的Step以及執行的規則
- 執行的流程可以設計邏輯，重點會在如何分割業務邏輯成為一個個的Step，也要考慮併行

### JobExecutionListener

- 可在Job執行時期的前後Log或進行些動作。
- 這裡有寫一個用來清除hsqldb的動作，每次執行新的Job就會清除資料，常保記憶體夠用。

## Step

- 一個Job內會有多個工作階段(Step)
- 一個工作階段最多各有一個Reader、Processor、Writer
- 定義chunk的大小(Step的群聚，到達chunk設定的大小就會執行write)
- 可設定listener做前後處理
- 可設定忽略原則判斷哪些錯誤可忽略或不可忽略
- ![架構參考](https://blog.codecentric.de/files/2012/03/Blog_Transactions_Listeners-1024x528.png)

### Listener
- 目前僅用到ItemProcessor，用來記錄處理前後的資料樣貌
- ![看圖](https://upload-images.jianshu.io/upload_images/5384456-635ef0821a2d799a.png)

### Reader
- 讀資料的角色，目前先用模擬的，看起來真正有I/O的只有初始化時，之後都是取記憶體內的資料
- 要設定為StepScope

### Processor
- 資料處理的核心

### Writer
- 資料處理完的動作，通常是寫資料庫，或推Queue、寫檔之類的輸出

## Skip
- 寫一個SkipPolicy，可以定義當遇到哪些例外的時候要中斷(不跳過)，哪些可跳過
- 要配合自定義例外

## Retry
- 目前只有放在ItemProcessor，寫得很簡單，是類別層級的，比較直覺，但範圍也較小
- 定義資料處理遇到什麼例外的時候需要重試(次數、延遲時間等設定)
- 要很小心recover方法的寫法，不然沒作用。
- 也可以設定在Step建造的時候，如同skipPolicy一樣，設定retryPolicy，也是捕捉例外

## Schedule
- 使用Spring的排程作法(會檢查排程是否進行,若已進行就不會發生作用)
- 如果是一次性的就不需要做排程(MainProcess)
- 這裡有一個重要的差別：排程是由主程序丟出一個執行緒來執行的
  - 排程進行的同時可以另外啟動一個Job去處理先前失敗的Job
  - JobRepository的清除會影響失敗Job的處理

## Scaling and Parallel
- 先擬定分散/非同步的策略，再決定採用SpringBatch的四種分散方式
- 以下是以非同步執行Step的方式進行分散，假設通常是Reader夠快，慢在資料處理以及寫入。
- chunk的大小影響分散的效果，需依實際執行狀況調整
- 簡述SpringBatch的四種分散策略
  - 多執行緒執行Step
    - 每個執行緒分別執行read / process / write 以chunk為單位
    - 因為以上組件會保存狀態,所以要特別注意(需為無狀態才能確保執行正常)
    - reader讀取同樣分量的數據,但有不同執行緒來取用
    - writer在每個執行緒的最後會呼叫一次
  - 單執行緒並行Step
    - 一個Job中會有許多Step，但有些Step彼此沒有相依，便可以並行縮短時間
  - 遠端步驟群(Remote Chunking)略
  - 資料分群
    - 一樣是多執行緒
    - 每個執行緒讀取的資料分開
    
### Multi-threaded Step
- 將Step進行非同步執行，但同一個chunk還是會等滿了再送出，也就是同一個chunk內的程序視為同步
- chunk的大小會影響分配taskExecutor的方式,也就是若chunk過大則會讓多筆資料放在同一條thread執行
- 要特別注意reader與writer的scope，設為stepScope可保證在step結束後就消滅