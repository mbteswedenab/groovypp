package groovy.remote

/**
 * Remode node in the claster
 */
@Typed(debug=true) class RemoteClusterNode extends MessageChannel<Serializable> {
   final RemoteConnection connection

   final UUID remoteId

   RemoteClusterNode (RemoteConnection connection, UUID remoteId) {
       this.connection = connection
       this.remoteId = remoteId
   }

   void post(Serializable message) {
        connection.send(new ToMainActor(payLoad:message))
   }

   @Typed(debug=true) public static class ToMainActor extends RemoteMessage {
        Serializable payLoad
   }
}