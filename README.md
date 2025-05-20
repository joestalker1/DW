Fault-tolerant Transfer Account Service with the advisory lock and Write Ahead Log to rollback the failed transactions effeciently.

- To run the application,please type './gradlew run'
- Added new endpoint (PUT) "/v1/accounts/transfer/{fromAccount}/{toAccount}?amount={number}"

# System design remarks
* To run the application,please type './gradlew run'
* Use advisory lock is scalable solution then the mutex is.
* Use retry with restricted timeout for acquiring advisory lock to eliminate thread starvation.
* GetAccount and Transfer money operations are fast operations.
* Unit-tests contains the concurrent tests as well.
* To rollback account balance,I used transferLog status,updateAt and Account updateAt fields.
* I propose to use Redis or Zookeeper to implement advisory lock, 
  that will be high available and resilient solution to make distributed locks.
* I am supposed to have two microservices instead of one AccountService:
  one is for write operation and one is for return balance for accounts.
  There exists writing and reading DB, where read db gets account events from transfer microservice
  by using messaging. Event looks like {'accountId':account1,'updateAt':DateTime,'operation':'add','amount': 10}
  It's better to send events because it doesn't need to care about message ordering
  (+ and - are commutative) but only check the field updateAt.
  It will give the eventual consistency for account balance but transfer operation
  would be fast and two microservices would be scalable independently.
  The NotificationService might be replaced by the AWS simple email service.
* I suppose to use SAGA for transfer money.


