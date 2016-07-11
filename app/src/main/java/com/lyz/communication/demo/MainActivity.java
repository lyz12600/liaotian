package com.lyz.communication.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.chat.EMCallStateChangeListener;
import com.easemob.chat.EMChat;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMMessage;
import com.easemob.chat.EMMessage.ChatType;
import com.easemob.chat.TextMessageBody;
import com.easemob.exceptions.EMServiceNotReadyException;


public class MainActivity extends Activity {

    private ListView listView;//接收文字消息
    private Button send;//发送文字按钮
    private EditText inputContentEditText;//接收编辑框中的文字消息
    private EMConversation conversation;
    private String toChatUsername = "aa";//接收方
    private DataAdapter adapter;

    private Button video;//发送语音请求
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        send = (Button) findViewById(R.id.btn_send);
        inputContentEditText = (EditText) findViewById(R.id.et_input);
        video = (Button) findViewById(R.id.btn_video);

        //获取对于的对话
        conversation = EMChatManager.getInstance().getConversation(toChatUsername);
        //连接适配器
        adapter = new DataAdapter();
        listView.setAdapter(adapter);

        //注册接收消息的监听广播
        //只有注册了广播才能接收到新消息，目前离线消息，在线消息都是走接收消息的广播
        //（离线消息目前无法监听，在登录以后，接收消息广播会执行一次拿到所有的离线消息）
        NewMessageBroadcastReceiver msgReceiver = new NewMessageBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(EMChatManager.getInstance().getNewMessageBroadcastAction());
        intentFilter.setPriority(3);
        registerReceiver(msgReceiver, intentFilter);

        //在 APP 中注册实时通话监听
        IntentFilter callFilter = new IntentFilter(EMChatManager.getInstance().getIncomingCallBroadcastAction());
        registerReceiver(new CallReceiver(), callFilter);

        //最后要通知sdk，UI 已经初始化完毕，注册了相应的receiver和listener, 可以接受broadcast了
        EMChat.getInstance().setAppInited();

        /**
         * 发送文字
         */
        //设置发送按钮
        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取到与聊天人的会话对象。参数username为聊天人的userid或者groupid，后文中的username皆是如此
                EMConversation conversation = EMChatManager.getInstance().getConversation(toChatUsername);
                //创建一条文本消息
                EMMessage message = EMMessage.createSendMessage(EMMessage.Type.TXT);
                //如果是群聊，设置chattype,默认是单聊

                //设置消息body
                TextMessageBody txtBody = new TextMessageBody(inputContentEditText.getText().toString());
                message.addBody(txtBody);
                //设置接收人
                message.setReceipt(toChatUsername);
                //把消息加入到此会话对象中
                conversation.addMessage(message);

                //发送时刷新listview保证发送的消息显示到UI上
                adapter.notifyDataSetChanged();
                listView.setAdapter(adapter);
                listView.setSelection(listView.getCount()-1);
                inputContentEditText.setText("");

                //发送消息
                EMChatManager.getInstance().sendMessage(message, new EMCallBack(){
                    @Override
                    public void onError(int arg0, String arg1) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "error", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                    @Override
                    public void onProgress(int arg0, String arg1) {
                    }

                    @Override
                    public void onSuccess() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }});
            }
        });
        /**
         * 发送视频
         */
        video.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * 设置通话状态监听
                 * @param listener
                 */
                EMChatManager.getInstance().addVoiceCallStateChangeListener(new EMCallStateChangeListener() {
                    @Override
                    public void onCallStateChanged(CallState callState, CallError error) {
                        switch (callState) {
                            case CONNECTING: // 正在连接对方
                                try {
                                    EMChatManager.getInstance().makeVideoCall(toChatUsername);
                                } catch (EMServiceNotReadyException e) {
                                    e.printStackTrace();
                                }
                                Toast.makeText(MainActivity.this, "正在连接", Toast.LENGTH_SHORT).show();
                                break;
                            case CONNECTED: // 双方已经建立连接
                                Toast.makeText(MainActivity.this,"建立成功", Toast.LENGTH_SHORT).show();
                                break;

                            case ACCEPTED: // 电话接通成功
                                Toast.makeText(MainActivity.this,"电话接通", Toast.LENGTH_SHORT).show();
                                break;
                            case DISCONNNECTED: // 电话断了
                                Toast.makeText(MainActivity.this,"电话断了", Toast.LENGTH_SHORT).show();
                                break;
                            case NETWORK_UNSTABLE: //网络不稳定
                                if(error == CallError.ERROR_NO_DATA){
                                    //无通话数据
                                    Toast.makeText(MainActivity.this,"网络不稳定1", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(MainActivity.this,"网络不稳定2", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case NETWORK_NORMAL: //网络恢复正常
                                Toast.makeText(MainActivity.this,"网络恢复正常", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                    }
                });
          }
        });
    }

    //在 APP 中注册实时通话监听
    private class CallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 拨打方username
            String from = intent.getStringExtra("from");
            // call type
            String type = intent.getStringExtra("type");

        }
    }

    private class NewMessageBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 注销广播
            abortBroadcast();

            // 消息id（每条消息都会生成唯一的一个id，目前是SDK生成）
            String msgId = intent.getStringExtra("msgid");
            //发送方
            String username = intent.getStringExtra("from");
            // 收到这个广播的时候，message已经在db和内存里了，可以通过id获取mesage对象
            EMMessage message = EMChatManager.getInstance().getMessage(msgId);
            EMConversation	conversation = EMChatManager.getInstance().getConversation(username);
            // 如果是群聊消息，获取到group id
            if (message.getChatType() == ChatType.GroupChat) {
                username = message.getTo();
            }
            if (!username.equals(username)) {
                // 消息不是发给当前会话，return
                return;
            }
            //刷新接收消息的UI
            conversation.addMessage(message);
            adapter.notifyDataSetChanged();
            listView.setAdapter(adapter);
            listView.setSelection(listView.getCount()-1);
        }
    }

    private class DataAdapter extends BaseAdapter{

        TextView textViewName;
        @Override
        public int getCount() {
            return conversation.getAllMessages().size();
        }

        @Override
        public Object getItem(int position) {
            return conversation.getAllMessages().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            EMMessage message = conversation.getAllMessages().get(position);
            TextMessageBody body = (TextMessageBody)message.getBody();
            if(message.direct == EMMessage.Direct.RECEIVE){
                if(message.getType() == EMMessage.Type.TXT){
                    convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.listitem_left, null);
                    textViewName = (TextView)convertView.findViewById(R.id.username);
                    textViewName.setText(message.getFrom());
                }
            }else{
                if(message.getType() == EMMessage.Type.TXT){
                    convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.listitem_right, null);
                }
            }
            TextView textViewContent = (TextView) convertView.findViewById(R.id.textcontent);
            textViewContent.setText(body.getMessage());

            return convertView;
        }

    }

}











