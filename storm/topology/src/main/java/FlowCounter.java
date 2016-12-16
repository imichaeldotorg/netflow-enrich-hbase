import com.google.gson.Gson;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;


public class FlowCounter extends BaseBasicBolt {

    private static final long serialVersionUID = 1L;

    int counter;
    Configuration conf;
    HTable hTable;
    OutputCollector collector;

    @Override
    public void prepare(Map stormConf, TopologyContext context) {
        this.counter = 0;

        conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "zookeeper");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.master", "hbase:60000");

        HTable hTable = null;
        try {
            hTable = new HTable(conf, "counters");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void execute(Tuple tuple, BasicOutputCollector collector) {
        if( counter % 10 == 0)  // Update HBase every 10 flows
        {
            try {
                Get g = new Get(toBytes("all_flows"));
                Result r = hTable.get(g);
                counter += new Integer(String.valueOf(r.getValue(toBytes("key"), toBytes("total_flows"))));
            } catch (IOException e) {}

            Put p = new Put(toBytes("all_flows"));
            p.add(toBytes("key"), toBytes("total_flows"), toBytes(counter));

            try {
                hTable.put(p);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("json"));
    }

}
