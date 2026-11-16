# تحقق الإصدار 5.6.9

## التغييرات

تفاصيل السلوك والنطاق في RELEASE_NOTES_5.6.9_AR.md. أُصلحت مواضع إدراج القراءات في القارئ، وأضيفت تبويبات الخدمات المحيطة بالقداس، واستُعيد عرض نص السَحَر العربي السليم بدل الملخص القديم، واختُصر تكرار أسماء المتكلمين. لم تُؤلّف صلوات جديدة أو تُترجم نصوص بين اللغات.

## النتائج المحلية

- نجح بناء assembleDebug وassembleRelease وbundleRelease للإصدار 5.6.9 / 50609.
- نجحت اختبارات Java: 93 اختبارًا، دون فشل أو تخطٍّ.
- نجح lintDebug وlintRelease: لا أخطاء مانعة؛ توجد 55 ملاحظة في كل تقرير.
- نجحت بوابة simple_quality_gate.py، وتشمل سلامة التقويم والمحرك اليومي والنصوص واللغات والتواقيع وفحص الأسرار.
- نجحت فحوص validate_liturgy_structure_strict.py وvalidate_liturgy_reader_integrity.py وvalidate_full_church_services.py وvalidate_play_store_metadata.py.
- مجموعة Python العامة: 705 ناجحة و16 فاشلة. أُعيد تشغيل الاختبارات الفاشلة على نسخة المصدر الأصلية 96cf1be وتأكد وجود الفشل نفسه فيها؛ لم تُعطّل الاختبارات ولم تُغيّر قواعد حجب النصوص الناقصة لإخفائها.
- لم يُجرَ اختبار واجهة على هاتف أو محاكي؛ بناء APK واختبارات الوحدات لا يعادلان هذا الاختبار.

## مصادر النصوص

فُحصت صفحة تحميل الصلوات الرسمية لمطرانية الأردن ووثيقة صلاة السَحَر DOCX المنشورة فيها. بصمة الوثيقة الحالية تطابق بصمة المصدر المسجلة في المشروع، وفقرات النص الـ145 موجودة فيها حرفيًا. بصمة وثيقة الساعات الحالية تطابق المصدر المسجل أيضًا. موقع Orthodox Online أعاد صفحة Bot Verification، ولم تُعامل كأنها نص قداس صالح.

هذه النتائج تثبت إصلاح التنظيم والتحقق التقني ضمن النصوص المتاحة؛ لا تمثل تصديقًا كنسيًا جديدًا حرفيًا لكل متغير طقسي أو اكتمال كل خدمة في كل يوم.

## إعادة البناء

في GitHub استخدم workflow الموجود Church Prayers، الذي يولّد أصول الكتاب المقدس والخدمات ثم يبني التطبيق. محليًا استُخدمت Java 17 وAndroid SDK 36 وGradle 8.13 مع التحقق من بصمات الأدوات وTLS.

للبناء المحلي أُعيد استخدام أصول bible/corpus وdata/church المولّدة من حزمة Church-Prayers للبناء الناجح 42887008188، المطابق للمصدر الأصلي 96cf1be. طُوبقت بصمة SHA-256 للحزمة مع بصمة GitHub قبل استخراجها، ولم تتغير مولّدات هذه الأصول. نجح فحص corpus الكامل بعد استخراجها. لذلك استُبعدت مهمتا prepareBibleCorpus وprepareChurchServiceCorpus من أمر البناء المحلي فقط؛ لم تُحذف اعتمادات التوليد من المشروع أو GitHub.

## حالة التوقيع والنشر الرسمي

APK المحلي المسمّى debug موقّع بمفتاح اختبار ويصلح للتجربة. ملف AAB وAPK بنمط release بُنيا دون مفتاح الإنتاج، ولا يصلحان كتحديث إنتاج موقّع أو للرفع المباشر إلى Google Play. قد يختلف توقيع APK التجريبي عن النسخة المثبتة، فلا يُقدّم كتحديث فوقها.

متغيرات مفتاح Android الأربعة غير محقونة في البيئة المحلية. قراءة أسماء أسرار GitHub رُفضت بصلاحية HTTP 403؛ لا يثبت ذلك عدم وجود أسرار في GitHub. حزمة APK السابقة من GitHub تحمل شهادة Android Debug.

تقرير التغطية المرفق بآخر بناء ناجح يسجل 9127 تذكارًا مسمى من 9131 يومًا، وأربعة سجلات عامة. workflow الحالي يحجب نشر Release رسمي حتى تكتمل هذه التغطية. لم تُعطّل هذه البوابة ولم يُنشر تحديث إنتاج غير موقّع.

## الاختبارات القديمة الفاشلة

- tests/test_2026_h2_lectionary.py::H22026LectionaryTests::test_compact_asset_and_android_wiring_pass_audit
- tests/test_430_completion.py::Completion430Tests::test_native_service_libraries_are_complete
- tests/test_fasting_guidance.py::FastingGuidanceTests::test_compact_annual_assets_keep_food_items_and_localized_detail
- tests/test_liturgy_routing_contract.py::LiturgyRoutingContractTests::test_calendar_routes_basil_and_presanctified_to_their_own_ids
- tests/test_r21_phase2_communion_offices.py::R21Phase2CommunionOfficeTests::test_arabic_thanksgiving_has_all_liturgy_specific_branches
- tests/test_r21_phase2_communion_offices.py::R21Phase2CommunionOfficeTests::test_no_cross_language_leakage_in_communion_services
- tests/test_r21_phase6_liturgy_service_selection.py::R21Phase6LiturgyServiceSelectionTests::test_basil_and_presanctified_editions_open_the_appointed_templates
- tests/test_r21_phase6_liturgy_service_selection.py::R21Phase6LiturgyServiceSelectionTests::test_contracts_register_complete_authorized_sources
- tests/test_r21_phase6_liturgy_service_selection.py::R21Phase6LiturgyServiceSelectionTests::test_dated_override_requires_documented_evidence
- tests/test_r21_phase6_liturgy_service_selection.py::R21Phase6LiturgyServiceSelectionTests::test_pascha_and_five_lenten_sundays_select_basil
- tests/test_r21_phase6_liturgy_service_selection.py::R21Phase6LiturgyServiceSelectionTests::test_presanctified_weekdays_and_holy_week
- tests/test_r21_phase7_native_liturgy_import_pipeline.py::R21Phase7NativeLiturgyImportTests::test_appointed_basil_card_opens_the_complete_native_template
- tests/test_r21_phase7_native_liturgy_import_pipeline.py::R21Phase7NativeLiturgyImportTests::test_basil_and_presanctified_are_displayable_from_authorized_sources
- tests/test_r21_phase8_finalization_pipeline.py::R21Phase8FinalizationTests::test_phase8_gate_reports_real_blockers
- tests/test_r35_compact_back_and_home_shortcuts.py::test_home_uses_six_shortcut_cards_and_one_context_aware_fasting_card
- tests/test_r66_simple_fasting_summary.py::test_home_fasting_notice_opens_the_simple_summary_route
