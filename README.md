# System design remarks
* Try to design Transfer money as strong consistence operation.
   Where read account balance operation may be snapshot consistence for very short time,because
   transfer money and lock service are working fast. Think it's better to consider 
   CQRS as one of using two db:one is for write and second is for read,here is 
   the trade-off between consistency and availability for read account operations.
* Used advisory lock approach as more scalable and fast operation then using mutex in Java.
* I tested TransferService rollback by hands when throws exception inside transfer code.
* I try to adjust https://github.com/openjdk/jcstress to prove thrad-safety of my code,
  but because of deadline,I couldn't finish it.
* It had better to Zookeeper for advisory lock in distributed system.
* My TransferLog is not good idea, but some SAGA implementation is the better.
