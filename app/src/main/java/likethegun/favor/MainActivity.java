package likethegun.favor;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

/**
 * Created by gimmiepepsi on 8/1/15.
 */
public class MainActivity extends AppCompatActivity{

    private final double MULTIPLIER = .05;
    private final DecimalFormat FORMAT = new DecimalFormat("#0.##%");
    private final String ZERO = "0%";
    private final String HUNDRED = "100%";
    private final String DC_FORMAT = "Roll >= %s + %s : %s\n";
    private final String[] ADDITIONAL_FAVORS = {"1d4", "1d6", "1d8", "1d10", "1d12"};

    private final int NORMAL = 0;
    private final int ADVANTAGE = 1;
    private final int DISADVANTAGE = 2;

    @Bind(R.id.main_probability_normal)
    TextView normalProbability;
    @Bind(R.id.main_probability_advantage)
    TextView advantageProbability;
    @Bind(R.id.main_probability_disadvantage)
    TextView disadvantageProbability;
    @Bind(R.id.main_dc)
    EditText dcView;
    @Bind(R.id.roll_die)
    EditText dieTotal;
    @Bind(R.id.roll_sides)
    EditText dieSides;
    @Bind(R.id.roll_modifier)
    EditText dieModifier;
    @Bind(R.id.additional_favor_add)
    View additionalFavorAdd;
    @Bind(R.id.additional_favor_remove)
    View additionalFavorRemove;
    @Bind(R.id.additional_favors)
    TextView additionalFavorsContainer;

    private HelpDice helpDice = new HelpDice();
    private AlertDialog additionalFavorsDialog;
    private boolean addAdditionalFavors = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        dcView.setText("10");

        additionalFavorsDialog = new AlertDialog.Builder(this)
                .setItems(ADDITIONAL_FAVORS, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(addAdditionalFavors){
                            helpDice.addHelp(new HelpDie(getSidesOfFavors(which)));
                        }
                        else{
                            helpDice.removeHelp(new HelpDie(getSidesOfFavors(which)));
                        }
                        updateAdditionalFavors();
                        calculateFavor();
                        dialog.dismiss();
                    }
                })
            .create();
    }

    private int getSidesOfFavors(int which){
        return Integer.parseInt(ADDITIONAL_FAVORS[which].split("1d")[1]);
    }

    @OnTextChanged({
            R.id.roll_die, R.id.roll_sides, R.id.roll_modifier, R.id.main_dc})
    public void onTextChanged(CharSequence text){
        calculateFavor();
    }

    public void calculateFavor(){
        try {
            boolean hasAdditionalDice = helpDice.getDieCount()>0;
            double dc = getValue(dcView) - getValue(dieModifier) - 1;
            normalProbability.setText("Normal:\n" + performCalculations(NORMAL, dc, hasAdditionalDice));
            advantageProbability.setText("Advantage:\n" + performCalculations(ADVANTAGE, dc, hasAdditionalDice));
            disadvantageProbability.setText("Disadvantage:\n" + performCalculations(DISADVANTAGE, dc, hasAdditionalDice));
        }
        catch (NumberFormatException e){
            //Skip it!
        }
    }

    @OnClick({R.id.additional_favor_add, R.id.additional_favor_remove})
    public void manageAdditionalFavors(View v){
        switch(v.getId()){
            case R.id.additional_favor_add:
                addAdditionalFavors = true;
                break;
            case R.id.additional_favor_remove:
                addAdditionalFavors = false;
                break;
        }

        additionalFavorsDialog.show();
    }

    private void updateAdditionalFavors(){
        if(helpDice.getDieCount() > 0) {
            StringBuilder builder = new StringBuilder("Currently adding:\n");
            for(HelpDie die : helpDice.helpDies){
                builder.append("1d").append(die.sides).append(" ");
            }
            additionalFavorsContainer.setText(builder.toString());
        }
        else{
            additionalFavorsContainer.setText("");
        }
    }

    private double findPercentage(int flag, double index){
        switch(flag){
            default:
            case NORMAL:
                return 1 - (MULTIPLIER * index);
            case ADVANTAGE:
                return 1 - (Math.pow(index / getValue(dieSides), 2));
            case DISADVANTAGE:
                return ((Math.pow(1 - (MULTIPLIER * index), 2)));
        }
    }

    private String performCalculations(int flag, double index, boolean hasAdditionalDice){
        return hasAdditionalDice ? formulateRange(flag, index) : FORMAT.format(findPercentage(flag, index));
    }

    private String formulateRange(int flag, double index){
        StringBuilder range = new StringBuilder();
        double rangeMax = index - helpDice.getDieCount() + 1;
        double rangeMin = rangeMax - helpDice.getMax() + helpDice.getDieCount();
        int y = helpDice.getMax() + 1;
        for(int i = (int)rangeMin; i <= rangeMax; i++) {
            y--;
            if(i <= 0){
                continue;
            }
            if(y < helpDice.getDieCount()){
                break;
            }
            if(i > 20){
                range.append(String.format(DC_FORMAT, i, y, ZERO));
            }
            else if (i < 0){
                range.append(String.format(DC_FORMAT, i, y, HUNDRED));
            }
            else{
                range.append(String.format(DC_FORMAT, i, y, FORMAT.format(
                        findPercentage(flag, i - (helpDice.getDieCount() > 0 ? 1 : 0)) *
                                (helpDice.getDieCount() > 1 ?
                                        getFavorProbability(y, helpDice.getAllSides()) :
                                        getSingleDieProbability(y, helpDice.getMax()))
                )));
            }
        }
        return range.toString();
    }


    private double getSingleDieProbability(int atLeast, int die){
        return 1 - ((atLeast-1)/(double)die);
    }

    private static double getFavorProbability(int atLeast, int... dice){
        List<Boolean> moravitz = new ArrayList<>();
        double denominator = 1;
        for(int die : dice){
            moravitz.add(atLeast - 1 <= die);
            denominator *= die;
        }
        double percentage = denominator - doTheT(atLeast - dice.length);

        for(int i = 0; i < moravitz.size(); i++){
            if(!moravitz.get(0)){
                percentage += doTheT(atLeast-dice[i]-dice.length);
            }
        }

        return percentage/denominator;
    }

    private static double doTheT(int n){
        return (n * (n + 1))/2;
    }

    private Integer getValue(EditText editText) throws NumberFormatException{
        return Integer.parseInt(editText.getText().toString());
    }

    private class HelpDie implements Comparable<HelpDie>{
        public int sides;

        public HelpDie(int sides) {
            this.sides = sides;
        }

        @Override
        public int compareTo(HelpDie another) {
            return sides - another.sides;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof HelpDie && sides == ((HelpDie)o).sides;
        }
    }

    private class HelpDice {

        public List<HelpDie> helpDies = new ArrayList<>();

        public HelpDice() {
        }

        public void addHelp(HelpDie die){
            helpDies.add(die);
            Collections.sort(helpDies);
        }

        public void removeHelp(HelpDie die){
            helpDies.remove(die);
            Collections.sort(helpDies);
        }

        public int[] getAllSides(){
            int[] sides = new int[helpDies.size()];
            int i = 0;
            for(HelpDie die : helpDies){
                sides[i] = die.sides;
                i++;
            }
            return sides;
        }

        public void resetHelp(){
            helpDies.clear();
        }

        public int getMax(){
            int max = 0;
            for(HelpDie die : helpDies){
                max += die.sides;
            }
            return max;
        }

        public int getDieCount(){
            return helpDies.size();
        }
    }

    //http://math.stackexchange.com/questions/1381685/probability-rolling-two-different-sided-die-and-sum-being-a-number/1381730#1381730
    //This is the original method for the d4/d6 question.
    //    private static double getFavorProbability(int dieA, int dieB, int atLeastThreshhold){
//        boolean one = atLeastThreshhold - 1 <= dieA;
//        boolean two = atLeastThreshhold - 1 <= dieB;
//
//        double denominator = dieA*dieB;
//        double minusTuna = denominator - doTheT(atLeastThreshhold - 2);
//
//        System.out.println("One " + one + " two " + two);
//        System.out.println("minus tuna " + minusTuna);
//
//        if(one && two){
//            return minusTuna/denominator;
//        }else if(!one && two){
//            return (minusTuna+doTheT(atLeastThreshhold-dieA-2))/denominator;
//        }else if(one && !two){
//            return (minusTuna+doTheT(atLeastThreshhold-dieB-2))/denominator;
//        }else if(!one && !two){
//            return (minusTuna+(doTheT(atLeastThreshhold-dieA-2)+doTheT(atLeastThreshhold-dieB-2)))/denominator;
//        }
//        else{
//            return 48.1516;
//        }
//    }
}
