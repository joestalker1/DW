# System design remarks
* Use advisory lock as faster and scalable solution.
* Use retry for acquiring lock to eliminate thread starvation.
* GetAccount and Transfer are fast operations.
* Unit-tests contains the concurrent tests as well.
* To rollback account balance,I used transferLog status,updateAt and Account updateAt fields.

