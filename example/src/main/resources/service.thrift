namespace java win.hgfdodo.thrift.client.example.thrift

service Echo{
  string ping();

  string echo(1: string input);
}