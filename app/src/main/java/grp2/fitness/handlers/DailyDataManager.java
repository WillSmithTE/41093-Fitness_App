package grp2.fitness.handlers;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.models.nosql.DailyDataDO;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.util.ArrayList;

import grp2.fitness.R;
import grp2.fitness.helpers.StringUtils;

public class DailyDataManager {

    public interface DailyDataListener{
        void onAllDailyDataSynced(ArrayList<DailyDataDO> allDailyData);
        void onDailyDataSaved(DailyDataDO dailyData);
    }

    public enum DailyDataColumn{DATE, HEART_RATE, ENERGY, STEPS, NICKNAME}

    private String userId;
    private String todayDate;

    private DynamoDBMapper dynamoDBMapper;

    private DailyDataDO dailyData;
    private ArrayList<DailyDataDO> allDailyData;
    private DailyDataListener callback;

    public DailyDataManager(String userId, DailyDataListener callback, String nickname){
        this.userId = userId;
        this.callback = callback;

        todayDate = StringUtils.getCurrentDateFormatted();
        dailyData = new DailyDataDO();
        dailyData.setUserId(userId);
        dailyData.setDate(todayDate);
        dailyData.setNickname(nickname);

        saveDailyData();

        dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(new AmazonDynamoDBClient(AWSMobileClient.getInstance().getCredentialsProvider()))
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();
    }

    private void syncDailyData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                dailyData = dynamoDBMapper.load(
                        DailyDataDO.class,
                        todayDate,
                        userId);

                saveDailyData();
            }
        }).start();
    }

    public String getColumn(DailyDataColumn column){
        String result = "";

        switch (column){
            case DATE:
                result = dailyData.getDate();
                break;
            case STEPS:
                result = dailyData.getSteps().toString();
                break;
            case ENERGY:
                result = dailyData.getEnergy().toString();
                break;
            case HEART_RATE:
                result = dailyData.getAverageHeartRate().toString();
                break;
            case NICKNAME:
                result = dailyData.getNickname();
                break;
        }

        return result;
    }

    public void setColumn(DailyDataColumn column, String value){
        switch (column){
            case DATE:
                dailyData.setDate(value);
                break;
            case STEPS:
                dailyData.setSteps(Double.parseDouble(value));
                break;
            case ENERGY:
                dailyData.setEnergy(Double.parseDouble(value));
                break;
            case HEART_RATE:
                dailyData.setAverageHeartRate(Double.parseDouble(value));
                break;
            case NICKNAME:
                dailyData.setNickname(value);
                break;
        }

        saveDailyData();
    }

    private void saveDailyData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(dynamoDBMapper != null){
                    dynamoDBMapper.save(dailyData);
                    callback.onDailyDataSaved(dailyData);
                }
            }
        }).start();
    }

    public void syncAllDailyData(final String date) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                allDailyData = new ArrayList<>(dynamoDBMapper.query(DailyDataDO.class, getAllDailyDataQuery(date)));
                callback.onAllDailyDataSynced(allDailyData);
            }
        }).start();
    }

    private DynamoDBQueryExpression<DailyDataDO> getAllDailyDataQuery(String date){
        DynamoDBQueryExpression<DailyDataDO> query = new DynamoDBQueryExpression<>();
        DailyDataDO hashObject = new DailyDataDO();
        hashObject.setDate(date);
        query.setHashKeyValues(hashObject);

        return query;
    }

    public void setCallback(DailyDataListener callback){
        this.callback = callback;
    }
}
