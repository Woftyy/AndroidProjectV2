package team.antelope.fg.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.ericliu.asyncexpandablelist.CollectionView;
import com.ericliu.asyncexpandablelist.async.AsyncExpandableListView;
import com.ericliu.asyncexpandablelist.async.AsyncExpandableListViewCallbacks;
import com.ericliu.asyncexpandablelist.async.AsyncHeaderViewHolder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import team.antelope.fg.R;
import team.antelope.fg.common.CircleTransform;
import team.antelope.fg.common.GlideApp;
import team.antelope.fg.constant.AccessNetConst;
import team.antelope.fg.constant.ForwardConst;
import team.antelope.fg.entity.NeedPreInfo;
import team.antelope.fg.entity.PublishNeed;
import team.antelope.fg.ui.MainActivity;
import team.antelope.fg.ui.base.BaseNearByActivity;
import team.antelope.fg.ui.business.NearbyBusiness;
import team.antelope.fg.ui.business.RetrofitServiceManager;
import team.antelope.fg.ui.fragment.NearbyFragment;
import team.antelope.fg.ui.presenter.impl.NearbyAsyncPersenterImpl;
import team.antelope.fg.util.DateUtil;
import team.antelope.fg.util.L;
import team.antelope.fg.util.P2PUtil;
import team.antelope.fg.util.PropertiesUtil;

/**
 * @Author hwc
 * @Date 2018/1/2
 * @TODO AsyncNeedActivity
 * 
 */
public class AsyncNeedActivity extends BaseNearByActivity implements AsyncExpandableListViewCallbacks<NeedPreInfo, PublishNeed> {

    private AsyncExpandableListView<NeedPreInfo, PublishNeed> mAsyncExpandableListView; //异步listview
    private CollectionView.Inventory<NeedPreInfo, PublishNeed> inventory;       // 同步Recycleview集合
    List<PublishNeed> needs = new ArrayList<>();
    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        //初始化控件
        mAsyncExpandableListView = (AsyncExpandableListView) findViewById(R.id.asyncExpandableCollectionView);
        //设置回调函数
        mAsyncExpandableListView.setCallbacks(this);

        if(isNetConnect){
            mPresenter.getServerNeedData(type, latitide, longitude);
        } else{
            mPresenter.getLocalNeedData();
        }
    }

    @Override
    protected void initPersenter() {
        mPresenter = new NearbyAsyncPersenterImpl(this);
    }

    /**
     * @Description 初始化presenter
     * @date 2017/12/27
     */


    @Override
    public int getLayout() {
        return R.layout.nearby_activity_async;
    }


    /*——————IView start——————*/

    @Override
    public void showNeedListData(List<NeedPreInfo> needs) {
        inventory = new CollectionView.Inventory<>();
//        for(){};    遍历
        if (needs == null || needs.size() == 0){
            return;
        }
        view_tags = new View[needs.size()];
        index = 0;
        for (int i = 0; i < needs.size(); i++) {
            NeedPreInfo NeedPreInfo = needs.get(i);
            CollectionView.InventoryGroup<NeedPreInfo, PublishNeed> group = inventory.newGroup(i); // groupOrdinal is the smallest, displayed first
            group.setHeaderItem(NeedPreInfo);
        }
        mAsyncExpandableListView.updateInventory(inventory);
    }

    /*——————IView end ——————*/


    @Override
    public void onStartLoadingGroup(int groupOrdinal) {
        needs = new ArrayList<>();  //清除原来的数据
        Long id = (Long) view_tags[groupOrdinal].getTag(R.id.tag_id);
        String endUrl = PropertiesUtil.getInstance().
                getProperty(AccessNetConst.GETPUBLISHNEEDENDPATH);
        final int index = groupOrdinal;
        L.i("TAG", "groupOrdinal:"+groupOrdinal+"  tag:" + id);
        Observable<PublishNeed> observable = RetrofitServiceManager.getInstance()
                .create(NearbyBusiness.class).getNearbyPublishNeed(endUrl, id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io()).delaySubscription(500, TimeUnit.MILLISECONDS);

        L.i("TAG", "observable:" + observable);
        addSubscription(observable.subscribe(new Subscriber<PublishNeed>() {
            @Override
            public void onCompleted() {
                L.i("TAG", "complete");
                if (mAsyncExpandableListView != null) {
                    mAsyncExpandableListView.onFinishLoadingGroup(index, needs);
                }
            }
            @Override
            public void onError(Throwable e) {
                L.i("TAG", "onError");
            }

            @Override
            public void onNext(PublishNeed publishNeed) {

                L.i("TAG", publishNeed.toString());
                needs.add(publishNeed);
            }
        }));
    }

    @Override
    public AsyncHeaderViewHolder newCollectionHeaderView(Context context, int groupOrdinal, ViewGroup parent) {
        // Create a new view.
        View v = LayoutInflater.from(context)
                .inflate(R.layout.nearby_header_row_item_async, parent, false);
        return new MyHeaderViewHolder(v, groupOrdinal, mAsyncExpandableListView);
    }

    @Override
    public RecyclerView.ViewHolder newCollectionItemView(Context context, int groupOrdinal, ViewGroup parent) {
        // Create a new view.
        View v = LayoutInflater.from(context)
                .inflate(R.layout.nearby_text_row_item_async, parent, false);
        return new NeedItemHolder(v);
    }

    @Override
    public void bindCollectionHeaderView(Context context, AsyncHeaderViewHolder holder, int groupOrdinal,
                                         NeedPreInfo headerItem) {

        MyHeaderViewHolder myHeaderViewHolder = (MyHeaderViewHolder) holder;
        //设置头像点击事件
        Bundle bundle = new Bundle();
        bundle.putLong(ForwardConst.USERID, headerItem.getUid());
        initEvent(this, myHeaderViewHolder.getRoundImg(), bundle);    //设置头像点击事件
        RequestOptions options = new RequestOptions();
        options.centerCrop()
                .placeholder(R.mipmap.default_avatar200)
                .error(R.mipmap.error200)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter()
                .transform(new CircleTransform(this));
        GlideApp.with(AsyncNeedActivity.this)
                .load(headerItem.getHeadimg())
                .apply(options)
                .into(myHeaderViewHolder.getRoundImg());
        myHeaderViewHolder.getTv_title().setText(headerItem.getTitle());
        myHeaderViewHolder.getTv_name().setText(headerItem.getName());
        myHeaderViewHolder.getTv_addressdesc().setText(headerItem.getAddressdesc());
        double lat2 = headerItem.getLatitude();
        double longt2 = headerItem.getLongitude();
        Double distance = P2PUtil.getExactDistance(this.latitide,this.longitude, lat2, longt2);
        int dis = distance.intValue();
        DecimalFormat df = new DecimalFormat("#.00");
        if(dis >= 1000){
            distance = distance/1000;
            String sDis = df.format(distance);
            myHeaderViewHolder.getTv_distance().setText("距离:" + sDis + "km");
        } else{
            myHeaderViewHolder.getTv_distance().setText("距离:" + dis + "m");
        }
        View view_tag = myHeaderViewHolder.getHeadView();
        view_tag.setTag(R.id.tag_id, headerItem.getId());    //打标签
        view_tags[index++] = view_tag;
        L.i("TAGg", "myHeaderViewHolder.getHeadView():" + view_tags[index-1].getTag(R.id.tag_id));
    }



    @Override
    public void bindCollectionItemView(Context context, RecyclerView.ViewHolder holder,
                                       int groupOrdinal, PublishNeed item) {
        NeedItemHolder needItemHolder = (NeedItemHolder) holder;
        needItemHolder.getTextViewTitle().setText(item.getTitle());
        needItemHolder.getTextViewDescrption().setText(item.getContent());
        needItemHolder.getTv_publish_time().setText(DateUtil.formatDataTime2(item.getCustomDate()));
        needItemHolder.getTv_stop_time().setText(DateUtil.formatDataTime2(item.getRequestDate()));
        needItemHolder.getTv_iscomplete().setText(item.isComplete() ? "是" : "否");
        needItemHolder.getTv_location().setText(item.getAddressDesc());
    }

    /**
     * @Author hwc
     * @Date 2017/12/27
     * @TODO AsyncSkillActivity  itemholder 静态内部类
     *
     */
    public static class NeedItemHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final TextView tv_publish_time;
        private final TextView tv_stop_time;
        private final TextView tv_iscomplete;
        private final TextView tv_location;


        public NeedItemHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
            tvTitle = v.findViewById(R.id.title);
            tvDescription = v.findViewById(R.id.description);
            tv_publish_time = v.findViewById(R.id.tv_publish_time);
            tv_stop_time = v.findViewById(R.id.tv_stop_time);
            tv_iscomplete = v.findViewById(R.id.tv_iscomplete);
            tv_location = v.findViewById(R.id.tv_location);
        }

        public TextView getTextViewTitle() {
            return tvTitle;
        }

        public TextView getTextViewDescrption() {
            return tvDescription;
        }

        public TextView getTv_publish_time(){
            return tv_publish_time;
        }
        public TextView getTv_stop_time(){
            return tv_stop_time;
        }
        public TextView getTv_iscomplete(){
            return tv_iscomplete;
        }
        public TextView getTv_location(){
            return tv_location;
        }
    }
}