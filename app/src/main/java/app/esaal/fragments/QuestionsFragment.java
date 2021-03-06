package app.esaal.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Text;

import java.util.ArrayList;

import app.esaal.MainActivity;
import app.esaal.R;
import app.esaal.adapters.FilterAdapter;
import app.esaal.adapters.QuestionsAdapter;
import app.esaal.classes.Constants;
import app.esaal.classes.GlobalFunctions;
import app.esaal.classes.Navigator;
import app.esaal.classes.RecyclerItemClickListener;
import app.esaal.classes.SessionManager;
import app.esaal.webservices.EsaalApiConfig;
import app.esaal.webservices.responses.questionsAndReplies.Question;
import app.esaal.webservices.responses.subjects.Subject;
import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class QuestionsFragment extends Fragment {
    public static FragmentActivity activity;
    public static QuestionsFragment fragment;
    private SessionManager sessionManager;
    private int questionPageIndex = 1;
    private int filterPageIndex = 1;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    private ArrayList<Subject> subjectsList = new ArrayList<>();
    private ArrayList<Question> questionsList = new ArrayList<>();
    private QuestionsAdapter questionsAdapter;
    private LinearLayoutManager layoutManager;

    @BindView(R.id.fragment_questions_cl_container)
    ConstraintLayout container;
    @BindView(R.id.fragment_questions_rv_questions)
    RecyclerView questions;
    @BindView(R.id.loading)
    ProgressBar loading;

    public static QuestionsFragment newInstance(FragmentActivity activity) {
        fragment = new QuestionsFragment();
        QuestionsFragment.activity = activity;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View childView = inflater.inflate(R.layout.fragment_questions, container, false);
        ButterKnife.bind(this, childView);
        return childView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (activity == null) {
            activity = getActivity();
        }
        MainActivity.setupAppbar(true, true, false, true, "account", getString(R.string.questionsAndReplies));
        sessionManager = new SessionManager(activity);
        GlobalFunctions.hasNewNotificationsApi(activity);

        FilterAdapter.subjectsSelectedIds.clear();
        layoutManager = new LinearLayoutManager(activity);
        questionsAdapter = new QuestionsAdapter(activity, questionsList);
        questions.setLayoutManager(layoutManager);
        questions.setAdapter(questionsAdapter);
        questionPageIndex = 1;
        isLastPage = false;
        isLoading = false;
        questionsApi();

        //change the color of editText in searchView
        EditText searchEditText = (EditText) MainActivity.search.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        searchEditText.setHintTextColor(getResources().getColor(R.color.colorPrimaryDark));

        MainActivity.search.setMaxWidth(Integer.MAX_VALUE);
        MainActivity.search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                MainActivity.search.onActionViewCollapsed();
                SearchFragment fragment = SearchFragment.newInstance(activity);
                Bundle b = new Bundle();
                b.putString("query", query);
                fragment.setArguments(b);
                Navigator.loadFragment(activity, fragment, R.id.activity_main_fl_container, true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
            }
        });

        MainActivity.filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLastPage = false;
                isLoading = false;
                subjectsApi();
            }
        });
    }

    public void filterPopUp(final ArrayList<Subject> subjectsList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View filterView = ((Activity) activity).getLayoutInflater().inflate(R.layout.custom_dialog_filter, null);
        RecyclerView filterRecycler = (RecyclerView) filterView.findViewById(R.id.custom_dialog_filter_rv_filterBy);
        TextView done = (TextView) filterView.findViewById(R.id.custom_dialog_filter_tv_done);
        filterRecycler.setLayoutManager(new GridLayoutManager(activity, 3));
        final FilterAdapter filterAdapter = new FilterAdapter(activity, subjectsList, null);
        filterRecycler.setAdapter(filterAdapter);
        builder.setCancelable(true);

        builder.setView(filterView);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setGravity(Gravity.CENTER);

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                questionsList.clear();
                String totalSelectedSubjects = "";
                for (Integer item : FilterAdapter.subjectsSelectedIds) {
                    if (FilterAdapter.subjectsSelectedIds.get(0) == item) {
                        totalSelectedSubjects = item + "";
                    } else {
                        totalSelectedSubjects = totalSelectedSubjects + "," + item;
                    }
                }
                filterPageIndex = 1;
                filterResultsApi(totalSelectedSubjects);
                dialog.cancel();
            }
        });
        filterRecycler.addOnItemTouchListener(new RecyclerItemClickListener(activity, filterRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (subjectsList.get(position).id == 0) {
                    questionsList.clear();
                    questionPageIndex = 1;
                    FilterAdapter.subjectsSelectedIds.clear();
                    questionsApi();
                    dialog.cancel();
                } else {
                    for (int i = 0; i < FilterAdapter.subjectsSelectedIds.size(); i++) {
                        if (FilterAdapter.subjectsSelectedIds.get(i) == subjectsList.get(position).id) {
                            FilterAdapter.subjectsSelectedIds.remove(i);
                            filterAdapter.notifyDataSetChanged();
                            return;
                        }
                    }
                    FilterAdapter.subjectsSelectedIds.add(subjectsList.get(position).id);
                    filterAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        }));


    }

    private void subjectsApi() {
        loading.setVisibility(View.VISIBLE);
        GlobalFunctions.DisableLayout(container);
        EsaalApiConfig.getCallingAPIInterface().userSubjects(
                sessionManager.getUserToken(),
                sessionManager.getUserId(),
                new Callback<ArrayList<Subject>>() {
                    @Override
                    public void success(ArrayList<Subject> subjects, Response response) {
                        loading.setVisibility(View.GONE);
                        GlobalFunctions.EnableLayout(container);
                        int status = response.getStatus();
                        if (status == 200) {
                            Subject all = new Subject();
                            all.setName(getString(R.string.all));
                            all.id = 0;
                            subjectsList.clear();
                            subjectsList.add(all);
                            subjectsList.addAll(subjects);
                            filterPopUp(subjectsList);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        GlobalFunctions.EnableLayout(container);
                        if (error.getResponse() != null && error.getResponse().getStatus() == 202) {
                            loading.setVisibility(View.GONE);
                            Snackbar.make(container, getString(R.string.noSubjects), Snackbar.LENGTH_SHORT).show();
                        } else {
                            loading.setVisibility(View.GONE);
                            Snackbar.make(container,getString(R.string.generalError), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void questionsApi() {
        EsaalApiConfig.getCallingAPIInterface().questions(
                sessionManager.getUserToken(),
                sessionManager.getUserId(),
                questionPageIndex,
                new Callback<ArrayList<Question>>() {
                    @Override
                    public void success(ArrayList<Question> questions, Response response) {
                        loading.setVisibility(View.GONE);
                        int status = response.getStatus();
                        if (status == 200) {
                            if (questions.size() > 0) {
                                questionsList.clear();
                                questionsList.addAll(questions);
                                questionsAdapter.notifyDataSetChanged();
                                //add Scroll listener to the recycler , for pagination
                                fragment.questions.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                    @Override
                                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                                        super.onScrollStateChanged(recyclerView, newState);
                                    }

                                    @Override
                                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                                        super.onScrolled(recyclerView, dx, dy);
                                        if (!isLastPage) {
                                            int visibleItemCount = layoutManager.getChildCount();

                                            int totalItemCount = layoutManager.getItemCount();

                                            int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                                /*isLoading variable used for check if the user send many requests
                                for pagination(make many scrolls in the same time)
                                1- if isLoading true >> there is request already sent so,
                                no more requests till the response of last request coming
                                2- else >> send new request for load more data (News)*/
                                            if (!isLoading) {

                                                if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                                    isLoading = true;

                                                    questionPageIndex = questionPageIndex + 1;

                                                    getMoreQuestions();

                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        } else {
                            isLastPage = true;
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        if (error.getResponse() != null && error.getResponse().getStatus() == 201) {
                            loading.setVisibility(View.GONE);
                            Snackbar.make(container, getString(R.string.noQuestions), Snackbar.LENGTH_SHORT).show();
                        } else {
                            loading.setVisibility(View.GONE);
                            Snackbar.make(container,getString(R.string.generalError), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void getMoreQuestions() {
        loading.setVisibility(View.VISIBLE);
        GlobalFunctions.DisableLayout(container);
        EsaalApiConfig.getCallingAPIInterface().questions(
                sessionManager.getUserToken(),
                sessionManager.getUserId(),
                questionPageIndex,
                new Callback<ArrayList<Question>>() {
                    @Override
                    public void success(ArrayList<Question> questions, Response response) {
                        loading.setVisibility(View.GONE);
                        GlobalFunctions.EnableLayout(container);
                        int status = response.getStatus();
                        if (status == 200) {
                            if (questions.size() > 0) {
                                questionsList.addAll(questions);
                                questionsAdapter.notifyDataSetChanged();

                            } else {
                                isLastPage = true;
                                questionPageIndex = questionPageIndex - 1;
                            }
                            isLoading = false;


                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        GlobalFunctions.EnableLayout(container);
                        loading.setVisibility(View.GONE);
                        Snackbar.make(container,getString(R.string.generalError), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterResultsApi(final String subjectIds) {
        loading.setVisibility(View.VISIBLE);
        EsaalApiConfig.getCallingAPIInterface().filterResult(
                sessionManager.getUserToken(),
                sessionManager.getUserId(),
                subjectIds,
                filterPageIndex,
                new Callback<ArrayList<Question>>() {
                    @Override
                    public void success(ArrayList<Question> questions, Response response) {
                        loading.setVisibility(View.GONE);
                        int status = response.getStatus();
                        if (status == 200) {
                            if (questions.size() > 0) {
                                questionsList.addAll(questions);
                                questionsAdapter.notifyDataSetChanged();
                                //add Scroll listener to the recycler , for pagination
                                fragment.questions.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                    @Override
                                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                                        super.onScrollStateChanged(recyclerView, newState);
                                    }

                                    @Override
                                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                                        super.onScrolled(recyclerView, dx, dy);
                                        if (!isLastPage) {
                                            int visibleItemCount = layoutManager.getChildCount();

                                            int totalItemCount = layoutManager.getItemCount();

                                            int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                                /*isLoading variable used for check if the user send many requests
                                for pagination(make many scrolls in the same time)
                                1- if isLoading true >> there is request already sent so,
                                no more requests till the response of last request coming
                                2- else >> send new request for load more data (News)*/
                                            if (!isLoading) {

                                                if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                                    isLoading = true;

                                                    filterPageIndex = filterPageIndex + 1;

                                                    getMoreFilterResults(subjectIds);

                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        } else {
                            isLastPage = true;
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        loading.setVisibility(View.GONE);
                        if (error.getResponse() != null && error.getResponse().getStatus() == 201) {
                            Snackbar.make(container, getString(R.string.noQuestions), Snackbar.LENGTH_SHORT).show();
                        } else {
                            loading.setVisibility(View.GONE);
                            Snackbar.make(container,getString(R.string.generalError), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void getMoreFilterResults(String subjectIds) {
        loading.setVisibility(View.VISIBLE);
        GlobalFunctions.DisableLayout(container);
        EsaalApiConfig.getCallingAPIInterface().filterResult(
                sessionManager.getUserToken(),
                sessionManager.getUserId(),
                subjectIds,
                filterPageIndex,
                new Callback<ArrayList<Question>>() {
                    @Override
                    public void success(ArrayList<Question> questions, Response response) {
                        loading.setVisibility(View.GONE);
                        GlobalFunctions.EnableLayout(container);
                        int status = response.getStatus();
                        if (status == 200) {
                            if (questions.size() > 0) {
                                questionsList.addAll(questions);
                                questionsAdapter.notifyDataSetChanged();

                            } else {
                                isLastPage = true;
                                filterPageIndex = filterPageIndex - 1;
                            }
                            isLoading = false;


                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        GlobalFunctions.EnableLayout(container);
                        loading.setVisibility(View.GONE);
                        Snackbar.make(container,getString(R.string.generalError), Snackbar.LENGTH_SHORT).show();
                    }
                }
        );
    }
}

