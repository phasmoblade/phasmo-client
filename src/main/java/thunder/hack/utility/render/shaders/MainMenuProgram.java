package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import thunder.hack.utility.render.animation.AnimationUtility;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import thunder.hack.utility.render.shaders.satin.api.managed.ShaderEffectManager;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform1f;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform2f;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform3f;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform4f;

import static thunder.hack.features.modules.Module.mc;

public class MainMenuProgram {
    private Uniform1f Time;
    private Uniform2f uSize;
    private Uniform4f color;
    private Uniform3f color1;
    private Uniform3f color2;
    public static float time_ = 10000f;

    public static final ManagedCoreShader MAIN_MENU = ShaderEffectManager.getInstance()
            .manageCoreShader(Identifier.of("thunderhack", "mainmenu"), VertexFormats.POSITION);

    public MainMenuProgram() {
        setup();
    }

    public void setParameters(float x, float y, float width, float height) {
        float i = (float) mc.getWindow().getScaleFactor();
        this.uSize.set(width * i, height * i);
        time_ += (float) (0.55 * AnimationUtility.deltaTime());
        this.Time.set((float) time_);
        // Устанавливаем чёрно-бело-серый цвет для шейдера
        // Используем монохромную палитру: чёрный, серый, белый
        float grayValue = 0.4f; // Средний серый
        this.color.set(grayValue, grayValue, grayValue, 1.0f);
    }
    
    public void setParameters(float x, float y, float width, float height, float r1, float g1, float b1, float r2, float g2, float b2) {
        float i = (float) mc.getWindow().getScaleFactor();
        this.uSize.set(width * i, height * i);
        time_ += (float) (0.55 * AnimationUtility.deltaTime());
        this.Time.set((float) time_);
        // Устанавливаем цвета из HudEditor
        this.color1.set(r1, g1, b1);
        this.color2.set(r2, g2, b2);
        // Устанавливаем чёрно-бело-серый цвет для шейдера
        float grayValue = 0.4f; // Средний серый
        this.color.set(grayValue, grayValue, grayValue, 1.0f);
    }

    public void use() {
        RenderSystem.setShader(MAIN_MENU::getProgram);
    }

    protected void setup() {
        uSize = MAIN_MENU.findUniform2f("uSize");
        Time = MAIN_MENU.findUniform1f("Time");
        color = MAIN_MENU.findUniform4f("color");
        color1 = MAIN_MENU.findUniform3f("color1");
        color2 = MAIN_MENU.findUniform3f("color2");
    }
}