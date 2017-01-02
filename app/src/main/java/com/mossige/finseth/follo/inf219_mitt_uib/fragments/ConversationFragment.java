package com.mossige.finseth.follo.inf219_mitt_uib.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.mossige.finseth.follo.inf219_mitt_uib.R;
import com.mossige.finseth.follo.inf219_mitt_uib.adapters.ConversationRecyclerViewAdapter;
import com.mossige.finseth.follo.inf219_mitt_uib.fragments.sending_message.ChooseRecipientFragment;
import com.mossige.finseth.follo.inf219_mitt_uib.listeners.EndlessRecyclerViewScrollListener;
import com.mossige.finseth.follo.inf219_mitt_uib.listeners.ItemClickSupport;
import com.mossige.finseth.follo.inf219_mitt_uib.listeners.MainActivityListener;
import com.mossige.finseth.follo.inf219_mitt_uib.models.Conversation;
import com.mossige.finseth.follo.inf219_mitt_uib.network.RequestQueueHandler;
import com.mossige.finseth.follo.inf219_mitt_uib.network.retrofit.MittUibClient;
import com.mossige.finseth.follo.inf219_mitt_uib.network.retrofit.ServiceGenerator;

import java.util.ArrayList;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by Follo on 15.03.2016.
 */
public class ConversationFragment extends Fragment {

    private static final String TAG = "ConversationFragment";

    private RecyclerView.Adapter mAdapter;
    private ArrayList<Conversation> conversations;

    /* If data is loaded */
    private boolean loaded;
    private SmoothProgressBar progressbar;

    private View rootView;
    MainActivityListener mCallback;
    // The URL to the next page in a request
    private String nextPage;

    public ConversationFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (MainActivityListener) context;
        }catch(ClassCastException e){
            Log.i(TAG, "onAttach: " + e.toString());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conversations = new ArrayList<>();
        loaded = false;
        nextPage = "";
        requestConversations();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_conversation, container, false);
        getActivity().setTitle(R.string.conversation_title);

        progressbar =  (SmoothProgressBar) rootView.findViewById(R.id.progressbar);
        initRecyclerView(rootView);
        if (loaded) {
            progressbar.setVisibility(View.GONE);
        } else {
            progressbar.setVisibility(View.VISIBLE);
        }

        return rootView;
    }

    private void requestConversations() {
        MittUibClient client = ServiceGenerator.createService(MittUibClient.class, getContext());
        Call<List<Conversation>> call;

        boolean firstPage = nextPage.isEmpty();
        if (firstPage) {
            call = client.getConversations();
        } else {
            call = client.getConversationsPagination(nextPage);
        }

        nextPage = "";
        call.enqueue(new Callback<List<Conversation>>() {
            @Override
            public void onResponse(Call<List<Conversation>> call, retrofit2.Response<List<Conversation>> response) {
                progressbar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    int currentSize = mAdapter.getItemCount();

                    conversations.addAll(response.body());
                    mAdapter.notifyItemRangeChanged(currentSize, conversations.size() - 1);

                    loaded = true;

                    String paginationLinks = response.headers().get("Link");
                    updatePageLink(paginationLinks);
                } else {
                    showSnackbar();
                }
            }

            @Override
            public void onFailure(Call<List<Conversation>> call, Throwable t) {
                progressbar.setVisibility(View.GONE);
                showSnackbar();
            }
        });
    }

    // TODO Solve in a more elegant and efficient way if possible.
    // TODO Move to a helper class as this will be used everywhere.
    private void updatePageLink(String paginationLinks) {
        String[] links = paginationLinks.split(",");

        for (int i = 0; i < links.length; i++) {
            String line = links[i];
            // If link contains rel=next
            if (line.contains("rel=\"next\"")) {

                // Get next link url
                String link = line.substring(line.indexOf("<") + 1);
                link = link.substring(0, link.indexOf(">"));

                nextPage = link;
                // There is only one 'rel=next' link in each response
                return;
            }
        }

        // Reset if there is no next link
        nextPage = "";
    }

    private void showSnackbar() {
        Snackbar snackbar = Snackbar.make(rootView.findViewById(R.id.coordinatorLayout), getString(R.string.error_conversation), Snackbar.LENGTH_LONG);
        snackbar.setDuration(4000);
        snackbar.setAction(getString(R.string.snackback_action_text), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestConversations();
            }
        });
        if (!snackbar.isShown()) {
            snackbar.show();
        }
    }

    @Override
    public void onPause() {
        // TODO Implement support for cancel of Retrofit requests.
        cancelRequest("conversations");
        super.onPause();
    }

    private void cancelRequest(final String tag) {
        RequestQueueHandler.getInstance(getContext()).getRequestQueue().cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                if (request.getTag() != null) {
                    return request.getTag().equals(tag);
                }
                return false;
            }
        });
    }


    private void initRecyclerView(View rootView) {
        // Create RecycleView
        // findViewById() belongs to Activity, so need to access it from the root view of the fragment
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        // Create the LayoutManager that holds all the views
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                // If there is a next link
                if (!nextPage.isEmpty()) {
                    requestConversations();
                }
            }
        });

        // Create adapter that binds the views with some content
        mAdapter = new ConversationRecyclerViewAdapter(conversations);
        recyclerView.setAdapter(mAdapter);

        initOnClickListener(recyclerView);
        initFabButton(rootView);
    }

    private void initFabButton(final View rootView) {
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                ChooseRecipientFragment chooseRecipientFragment = new ChooseRecipientFragment();
                transaction.replace(R.id.content_frame, chooseRecipientFragment);
                transaction.addToBackStack(null);

                transaction.commit();
            }
        });
    }

    private void initOnClickListener(RecyclerView recyclerView) {
        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                SingleConversationFragment singleConversationFragment = new SingleConversationFragment();
                transaction.replace(R.id.content_frame, singleConversationFragment);
                transaction.addToBackStack(null);

                //Bundles all parameters needed for showing one announcement
                Bundle args = new Bundle();
                args.putInt("conversationID", conversations.get(position).getId());
                singleConversationFragment.setArguments(args);

                transaction.commit();
            }
        });
    }
}
