package tripPlannerAgent.agents;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.util.JarSkillRepositoryAdapter;
import io.agentscope.core.tool.Toolkit;
import tripPlannerAgent.tool.Calculate;
import utils.AgentUtils;

import java.io.IOException;

/**
 * author: Imooc
 * description: 景点推荐Agent
 * date: 2026
 */

public class SuggestSightAgent {

    //创建景点推荐Agent
    public ReActAgent getSuggestSightAgent() {


        Toolkit toolkit = new Toolkit();
        //构建Skill，并将工具包和Skill结合
        SkillBox skillBox = new SkillBox(toolkit);

        //以文件形式读取Skill.md
        JarSkillRepositoryAdapter repo = null;
        try {
            repo = new JarSkillRepositoryAdapter(
                    "skills"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //景点推荐技能
        AgentSkill SuggestSightsSkill = repo.getSkill("Suggest-Sights");
        //表格制作技能
        //AgentSkill MakeATableSkill = repo.getSkill("Make-A-Table");
        skillBox.registerSkill(SuggestSightsSkill);
        //skillBox.registerSkill(MakeATableSkill);

        //注册工具
        skillBox.registration().tool(new Calculate());


        ReActAgent.Builder builder = AgentUtils.getReActAgentBuilder(
                        "SuggestSightAgent",
                        "擅长景点推荐"
                )
                //挂载工具包
                .toolkit(toolkit)
                //挂载Skills
                .skillBox(skillBox)

                ;

        return builder.build();
    }
}
