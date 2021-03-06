package no.mofifo.imber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import no.mofifo.imber.R;
import no.mofifo.imber.adapters.MessageRecyclerViewAdapter;
import no.mofifo.imber.fragments.sending_message.ComposeMessageFragment;
import no.mofifo.imber.listeners.MainActivityListener;
import no.mofifo.imber.models.Conversation;
import no.mofifo.imber.models.Message;
import no.mofifo.imber.models.Participant;
import no.mofifo.imber.retrofit.MittUibClient;
import no.mofifo.imber.retrofit.ServiceGenerator;

import java.util.ArrayList;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import retrofit2.Call;
import retrofit2.Callback;


/**
 * Created by Follo on 15.03.2016.
 */
public class SingleConversationFragment extends Fragment {

    private static final String TAG = "ConversationFragment";

    private MessageRecyclerViewAdapter mAdapter;

    private SmoothProgressBar progressbar;

    private Conversation conversation;
    private ArrayList<Message> messages;

    private boolean loaded;

    MainActivityListener mCallback;
    private View rootView;
    private FloatingActionButton fab;
    private MittUibClient mittUibClient;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try{
            mCallback = (MainActivityListener) context;
        }catch(ClassCastException e){
            Log.i(TAG, "onAttach: " + e.toString());
        }

        mittUibClient = ServiceGenerator.createService(MittUibClient.class, context);
    }

    public SingleConversationFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        messages = new ArrayList<>();
        loaded = false;

        requestSingleConversation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_conversation, container, false);

        progressbar = (SmoothProgressBar) rootView.findViewById(R.id.progressbar);
        initRecycleView();
        initFabButton();

        if (loaded) {
            fab.setEnabled(true);
            progressbar.setVisibility(View.GONE);
        } else {
            fab.setEnabled(false);
            progressbar.setVisibility(View.VISIBLE);
        }

        return rootView;
    }

    private void requestSingleConversation() {
        if (getArguments().containsKey("conversationID")) {
            int conversationId = getArguments().getInt("conversationID");

            Call<Conversation> call = mittUibClient.getConversation(conversationId);
            call.enqueue(new Callback<Conversation>() {
                @Override
                public void onResponse(Call<Conversation> call, retrofit2.Response<Conversation> response) {
                    if (response.isSuccessful()) {

                        conversation = response.body();
                        mAdapter.setParticipants(conversation.getParticipants());

                        fab.setEnabled(true);
                        messages.clear();
                        messages.addAll(conversation.getMessages());

                        if (getActivity() != null) {
                            getActivity().setTitle(conversation.getSubject());
                        }

                        loaded = true;
                        mAdapter.notifyDataSetChanged();

                        if (isAdded()) {
                            progressbar.progressiveStop();
                        }

                    }
                }

                @Override
                public void onFailure(Call<Conversation> call, Throwable t) {
                    showSnackbar();
                }
            });

        } else {
            showSnackbar();
        }
    }

    private void showSnackbar() {
        progressbar.progressiveStop();
        Snackbar snackbar = Snackbar.make(rootView.findViewById(R.id.coordinatorLayout), getString(R.string.error_conversation), Snackbar.LENGTH_LONG);
        snackbar.setDuration(4000); // Gives false syntax error
        snackbar.setAction(getString(R.string.snackback_action_text), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSingleConversation();
            }
        });
        if (!snackbar.isShown()) {
            snackbar.show();
        }
    }

    private void initFabButton() {
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_reply_white_24dp, null));
        setFabListener();
    }

    private void setFabListener() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                ComposeMessageFragment composeMessageFragment = new ComposeMessageFragment();
                transaction.replace(R.id.content_frame, composeMessageFragment);

                Bundle bundle = new Bundle();

                ArrayList<Integer> ids = new ArrayList<>();
                for (Participant p : conversation.getParticipants()) {
                    ids.add(p.getId());
                }

                bundle.putIntegerArrayList("recipientIDs", ids);
                composeMessageFragment.setArguments(bundle);

                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }

    private void initRecycleView() {
        // Create RecycleView
        // findViewById() belongs to Activity, so need to access it from the root view of the fragment
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        // Create the LayoutManager that holds all the views
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        // Create adapter that binds the views with some content
        mAdapter = new MessageRecyclerViewAdapter(messages);
        recyclerView.setAdapter(mAdapter);
    }
}
