package app.esaal.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.esaal.MainActivity;
import app.esaal.R;
import app.esaal.classes.GlobalFunctions;
import app.esaal.classes.Navigator;
import app.esaal.classes.SessionManager;
import app.esaal.fragments.QuestionDetailsFragment;
import app.esaal.webservices.responses.questionsAndReplies.Question;
import butterknife.BindView;
import butterknife.ButterKnife;

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.viewHolder> {
    private Context context;
    private ArrayList<Question> questionsList;
    private SessionManager sessionManager;

    public QuestionsAdapter(Context context, ArrayList<Question> questionsList) {
        this.context = context;
        this.questionsList = questionsList;
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.item_question_cl_container)
        ConstraintLayout container;
        @BindView(R.id.item_question_iv_avatar)
        ImageView avatar;
        @BindView(R.id.item_question_tv_subjectName)
        TextView subjectName;
        @BindView(R.id.item_question_tv_questionText)
        TextView questionText;
        @BindView(R.id.item_question_tv_date)
        TextView date;
        @BindView(R.id.item_question_tv_esaalStudent)
        TextView esaalStudent;

        //        @BindView(R.id.item_question_iv_reply)
//        ImageView reply;
//        @BindView(R.id.item_question_tv_replyText)
//        TextView replyText;
        @BindView(R.id.item_question_v_details)
        View details;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            sessionManager = new SessionManager(context);
            ButterKnife.bind(this, itemView);
        }
    }

    @NonNull
    @Override
    public QuestionsAdapter.viewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View childView = LayoutInflater.from(context).inflate(R.layout.item_question, viewGroup, false);
        return new QuestionsAdapter.viewHolder(childView);
    }

    @Override
    public void onBindViewHolder(@NonNull final QuestionsAdapter.viewHolder viewHolder, final int position) {
//        if(!sessionManager.isTeacher()){
//            viewHolder.reply.setVisibility(View.GONE);
//            viewHolder.replyText.setVisibility(View.GONE);
//        }
        viewHolder.subjectName.setText(questionsList.get(position).subject.getName());
        if (questionsList.get(position).description.length() > 25)
            viewHolder.questionText.setText(questionsList.get(position).description.substring(0, 25) + "...");
        else
            viewHolder.questionText.setText(questionsList.get(position).description);

        if (questionsList.get(position).isPending && (questionsList.get(position).pendingUserId == sessionManager.getUserId())) {
            viewHolder.container.setBackground(context.getDrawable(R.mipmap.box_reservation_question));
            viewHolder.date.setTextColor(Color.parseColor("#ffffff"));
            viewHolder.esaalStudent.setTextColor(Color.parseColor("#ffffff"));
            viewHolder.questionText.setTextColor(Color.parseColor("#ffffff"));
            viewHolder.subjectName.setTextColor(Color.parseColor("#ffffff"));
            viewHolder.avatar.setImageResource(R.mipmap.ic_student_white);
        } else {
            viewHolder.container.setBackground(context.getDrawable(R.mipmap.box_search_res));
            viewHolder.date.setTextColor(Color.parseColor("#9E9D9D"));
            viewHolder.esaalStudent.setTextColor(Color.parseColor("#0A68B0"));
            viewHolder.questionText.setTextColor(Color.parseColor("#000000"));
            viewHolder.subjectName.setTextColor(Color.parseColor("#000000"));
            viewHolder.avatar.setImageResource(R.mipmap.ic_student);
        }
        viewHolder.date.setText(GlobalFunctions.formatDateAndTime(questionsList.get(position).creationDate));
        viewHolder.details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigator.loadFragment((FragmentActivity) context, QuestionDetailsFragment.newInstance((FragmentActivity) context, questionsList.get(position).id), R.id.activity_main_fl_container, true);
            }
        });

        if (MainActivity.isEnglish) {
            Typeface enBold = Typeface.createFromAsset(context.getAssets(), "montserrat_medium.ttf");
            viewHolder.subjectName.setTypeface(enBold);
            viewHolder.esaalStudent.setTypeface(enBold);
        } else {
            Typeface arBold = Typeface.createFromAsset(context.getAssets(), "cairo_bold.ttf");
            Typeface cairo = Typeface.createFromAsset(context.getAssets(), "cairo_regular.ttf");
            viewHolder.subjectName.setTypeface(arBold);
            viewHolder.esaalStudent.setTypeface(arBold);
            viewHolder.questionText.setTypeface(cairo);
        }
    }

    @Override
    public int getItemCount() {
        return questionsList.size();
    }
}








