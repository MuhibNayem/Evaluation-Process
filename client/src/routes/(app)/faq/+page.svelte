<script lang="ts">
    import * as Card from "$lib/components/ui/card/index.js";
    import HelpInfo from "$lib/components/help-info.svelte";
</script>

<div class="space-y-6 pb-20">
    <div>
        <h1 class="text-3xl font-bold tracking-tight">FAQ & Scenarios</h1>
        <p class="text-muted-foreground">
            Guided runbooks mapped to exact UI screens for admins and users.
        </p>
    </div>

    <Card.Root>
        <Card.Header>
            <div class="flex items-center gap-1">
                <Card.Title>Navigation Guide (Where To Do What)</Card.Title>
                <HelpInfo
                    title="UI Navigation Map"
                    what="Maps each responsibility to a specific page in the app."
                    why="Users should know exactly where to go next."
                    how="Follow these paths in the sidebar in order."
                    example="Audience -> Admin Rules -> Campaigns -> Reports"
                />
            </div>
        </Card.Header>
        <Card.Content class="space-y-2 text-sm">
            <div class="overflow-auto rounded-md border">
                <table class="min-w-full text-sm">
                    <thead class="bg-muted/30">
                        <tr>
                            <th class="px-3 py-2 text-left">Goal</th>
                            <th class="px-3 py-2 text-left">Page</th>
                            <th class="px-3 py-2 text-left">What To Do There</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr class="border-t">
                            <td class="px-3 py-2 font-medium">Load people/sections data</td>
                            <td class="px-3 py-2"><a class="underline" href="/audience">Audience</a></td>
                            <td class="px-3 py-2">Configure source type, ingest, monitor runs, replay failures.</td>
                        </tr>
                        <tr class="border-t">
                            <td class="px-3 py-2 font-medium">Configure assignment logic</td>
                            <td class="px-3 py-2"><a class="underline" href="/admin/rules">Admin Rules</a></td>
                            <td class="px-3 py-2">Create/update rules, validate/simulate, approve/publish workflow.</td>
                        </tr>
                        <tr class="border-t">
                            <td class="px-3 py-2 font-medium">Create and operate term cycle</td>
                            <td class="px-3 py-2"><a class="underline" href="/campaigns">Campaigns</a></td>
                            <td class="px-3 py-2">Create campaign, activate/close/archive, reconcile/backfill assignments.</td>
                        </tr>
                        <tr class="border-t">
                            <td class="px-3 py-2 font-medium">Manage platform flags</td>
                            <td class="px-3 py-2"><a class="underline" href="/admin/settings">Settings</a></td>
                            <td class="px-3 py-2">Update system keys and per-campaign overrides.</td>
                        </tr>
                        <tr class="border-t">
                            <td class="px-3 py-2 font-medium">Export and inspect outcomes</td>
                            <td class="px-3 py-2"><a class="underline" href="/reports">Reports</a></td>
                            <td class="px-3 py-2">View campaign JSON, export CSV/PDF reports.</td>
                        </tr>
                        <tr class="border-t">
                            <td class="px-3 py-2 font-medium">Complete assigned forms</td>
                            <td class="px-3 py-2"><a class="underline" href="/evaluations">My Evaluations</a></td>
                            <td class="px-3 py-2">Evaluatees/evaluators submit evaluations only.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <p class="text-muted-foreground">Role boundary: Admin configures system. Evaluatees do not configure audience/rules/campaigns.</p>
        </Card.Content>
    </Card.Root>

    <Card.Root>
        <Card.Header>
            <Card.Title>Step-By-Step Admin Runbook</Card.Title>
            <Card.Description>Use this sequence for every new cycle.</Card.Description>
        </Card.Header>
        <Card.Content class="space-y-4 text-sm">
            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Step 1: Prepare Audience Data</summary>
                <div class="mt-3 space-y-2 text-sm text-muted-foreground">
                    <p>Go to <a class="underline" href="/audience">Audience</a>.</p>
                    <p>Select source type: JSON, CSV, REST, or JDBC.</p>
                    <p>Create/choose mapping profile so source fields map to canonical fields.</p>
                    <p>Run dry-run ingestion first, then live ingestion.</p>
                    <p>If failed rows exist, open run details, fix source data, replay.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Step 2: Configure Rule Logic</summary>
                <div class="mt-3 space-y-2 text-sm text-muted-foreground">
                    <p>Go to <a class="underline" href="/admin/rules">Admin Rules</a>.</p>
                    <p>Create or update rule definition with selected rule type and config.</p>
                    <p>Run simulation with diagnostics enabled.</p>
                    <p>Create publish request and approve/reject via workflow controls.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Step 3: Create and Launch Campaign</summary>
                <div class="mt-3 space-y-2 text-sm text-muted-foreground">
                    <p>Go to <a class="underline" href="/campaigns/new">Campaigns &gt; New Campaign</a>.</p>
                    <p>Set template, timeline, and audience source strategy.</p>
                    <p>Activate campaign when ready.</p>
                    <p>Publish assignments from Admin Rules page to this campaign.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Step 4: Monitor and Report</summary>
                <div class="mt-3 space-y-2 text-sm text-muted-foreground">
                    <p>Use <a class="underline" href="/campaigns">Campaigns</a> reconciliation/backfill tools when needed.</p>
                    <p>Use <a class="underline" href="/reports">Reports</a> for campaign JSON/CSV/PDF export.</p>
                    <p>Close campaign after deadline; archive when complete.</p>
                </div>
            </details>
        </Card.Content>
    </Card.Root>

    <Card.Root>
        <Card.Header>
            <Card.Title>Comprehensive Scenarios</Card.Title>
            <Card.Description>Detailed examples by operating model.</Card.Description>
        </Card.Header>
        <Card.Content class="space-y-4 text-sm">
            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Scenario A: Semester Instructor Evaluation</summary>
                <div class="mt-2 space-y-2 text-muted-foreground">
                    <p>One-time: configure source, mapping profile, and rule template once.</p>
                    <p>Each semester: ingest latest students/instructors/sections, create campaign, simulate, publish assignments.</p>
                    <p>Students only open <a class="underline" href="/evaluations">My Evaluations</a> and submit forms.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Scenario A1: 9 Sections, 4 Instructors, Students Taking Multiple Courses</summary>
                <div class="mt-2 space-y-2 text-muted-foreground">
                    <p><strong>Business meaning:</strong> section is the assignment boundary. A student evaluates only instructor(s) of the sections they are enrolled in.</p>
                    <p><strong>Expected result:</strong> if one student is enrolled in 4 course sections, that student gets 4 instructor evaluations.</p>

                    <div class="rounded-md border p-3">
                        <p class="font-medium text-foreground mb-1">1) Data model to configure (Audience page)</p>
                        <p><strong>PERSON:</strong> every student and instructor. Required identifier: <code>person_id</code>.</p>
                        <p><strong>GROUP:</strong> every section as one group (example: <code>CSE101-SEC01</code>).</p>
                        <p><strong>MEMBERSHIP:</strong> relation rows linking person to section with role.</p>
                        <p>Membership roles should clearly distinguish student vs instructor, for example: <code>STUDENT</code>, <code>INSTRUCTOR</code>.</p>
                    </div>

                    <div class="rounded-md border p-3">
                        <p class="font-medium text-foreground mb-1">2) Mapping profile meaning (Audience page)</p>
                        <p>Map source system keys into canonical keys used by ingestion validation.</p>
                        <p>Typical mappings:</p>
                        <p><code>student_id -> person_id</code>, <code>teacher_id -> person_id</code>, <code>section_code -> group_id</code>, <code>course_title -> group_name</code>.</p>
                        <p>Use separate mapping profiles if source formats differ (CSV vs JDBC vs REST).</p>
                    </div>

                    <div class="rounded-md border p-3">
                        <p class="font-medium text-foreground mb-1">3) Rule intent to configure (Admin Rules page)</p>
                        <p>Rule must match evaluator/evaluatee within the same section-group context.</p>
                        <p><strong>Evaluator side:</strong> membership role = STUDENT.</p>
                        <p><strong>Evaluatee side:</strong> membership role = INSTRUCTOR.</p>
                        <p><strong>Scope:</strong> same <code>group_id</code> (section only), not across sections.</p>
                        <p>Run simulation with diagnostics before publish to verify exclusions and pair counts.</p>
                    </div>

                    <div class="rounded-md border p-3">
                        <p class="font-medium text-foreground mb-1">4) Semester operation (Campaigns + Admin Rules)</p>
                        <p>Per semester sequence:</p>
                        <p>Ingest PERSON/GROUP/MEMBERSHIP -> Create semester campaign -> Simulate rule (dry-run) -> Publish assignments.</p>
                        <p>New sections/courses/students are handled by ingestion; no evaluatee-side configuration is needed.</p>
                        <p>For add/drop changes during semester, re-ingest updated memberships and republish based on policy.</p>
                    </div>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Scenario B: Mid-Semester Add/Drop</summary>
                <div class="mt-2 space-y-2 text-muted-foreground">
                    <p>Re-ingest affected audience slice.</p>
                    <p>Simulate rule in dry-run to inspect diff.</p>
                    <p>Publish with replace disabled unless full reassignment is intentionally required.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Scenario C: Department Peer Review</summary>
                <div class="mt-2 space-y-2 text-muted-foreground">
                    <p>Use peer assignment rule type and add exclusions/self-avoidance config.</p>
                    <p>Run diagnostics in simulation to verify why candidates are excluded.</p>
                    <p>Publish once sample outputs match expected policy.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Scenario D: Manager-to-Direct-Report Review</summary>
                <div class="mt-2 space-y-2 text-muted-foreground">
                    <p>Ensure ingestion includes hierarchy attributes (manager_id, person_id).</p>
                    <p>Map these fields in profile, then use rule config that targets reporting lines.</p>
                    <p>Dry-run on a limited campaign first, then scale.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Scenario E: Multi-Source Audience (CSV + REST + JDBC)</summary>
                <div class="mt-2 space-y-2 text-muted-foreground">
                    <p>Create one mapping profile per source type.</p>
                    <p>Normalize all sources into canonical schema before assignment publish.</p>
                    <p>For JDBC, use configured <code>connectionRef</code> values only.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Scenario F: Emergency Rule Patch</summary>
                <div class="mt-2 space-y-2 text-muted-foreground">
                    <p>Clone or update rule with version bump.</p>
                    <p>Simulate with diagnostics enabled and compare with previous behavior.</p>
                    <p>Use approval workflow comments to document reason and impact.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Scenario G: Campaign Re-open / Late Submissions</summary>
                <div class="mt-2 space-y-2 text-muted-foreground">
                    <p>Adjust campaign settings/override in <a class="underline" href="/admin/settings">Settings</a>.</p>
                    <p>Re-open only with explicit governance decision.</p>
                    <p>Re-export reports after closure window change.</p>
                </div>
            </details>
        </Card.Content>
    </Card.Root>

    <Card.Root>
        <Card.Header>
            <Card.Title>Audience Modeling by Scenario</Card.Title>
            <Card.Description>How to model PERSON/GROUP/MEMBERSHIP correctly for different use cases.</Card.Description>
        </Card.Header>
        <Card.Content class="space-y-4 text-sm text-muted-foreground">
            <div class="rounded-md border p-3">
                <p class="font-medium text-foreground mb-1">Core modeling rule</p>
                <p>
                    Keep <code>PERSON</code> as people, <code>GROUP</code> as assignment boundary, and
                    <code>MEMBERSHIP</code> as person-to-boundary link with role.
                </p>
                <p>
                    If assignments should happen "inside section/team/reporting-line", that boundary must be represented in
                    <code>GROUP</code> and referenced by <code>MEMBERSHIP</code>.
                </p>
            </div>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium text-foreground">Scenario Blueprint 1: Students evaluate instructors per section</summary>
                <div class="mt-2 space-y-2">
                    <p><strong>PERSON:</strong> students + instructors.</p>
                    <p><strong>GROUP:</strong> each section (example <code>CSE101-SEC01</code>).</p>
                    <p><strong>MEMBERSHIP:</strong> student->section role <code>STUDENT</code>, instructor->section role <code>INSTRUCTOR</code>.</p>
                    <p><strong>Rule intent:</strong> evaluator role STUDENT, evaluatee role INSTRUCTOR, same <code>group_id</code> only.</p>
                    <p><strong>Outcome:</strong> student with 4 enrolled sections gets 4 evaluations.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium text-foreground">JDBC Query Templates: Student-Teacher Per Section</summary>
                <div class="mt-2 space-y-3">
                    <p>Run ingestion in 3 steps using <code>entityType</code>: <code>PERSON</code>, <code>GROUP</code>, <code>MEMBERSHIP</code>.</p>

                    <div>
                        <p class="font-medium text-foreground mb-1">1) PERSON query (students + instructors)</p>
                        <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto"><code>{`select distinct
  p.person_id::text as person_id,
  p.full_name as display_name,
  p.email as email,
  true as active
from people p
where p.active = true;`}</code></pre>
                    </div>

                    <div>
                        <p class="font-medium text-foreground mb-1">2) GROUP query (one group per section per term)</p>
                        <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto"><code>{`select
  (c.course_code || '-' || s.section_no || '-' || s.term_code)::text as group_id,
  'COURSE_SECTION'::text as group_type,
  (c.course_code || ' Sec ' || s.section_no || ' (' || s.term_code || ')')::text as group_name,
  s.section_id::text as external_ref,
  true as active
from sections s
join courses c on c.id = s.course_id
where s.active = true;`}</code></pre>
                    </div>

                    <div>
                        <p class="font-medium text-foreground mb-1">3) MEMBERSHIP query (student + instructor links)</p>
                        <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto"><code>{`select
  e.student_id::text as person_id,
  (c.course_code || '-' || s.section_no || '-' || s.term_code)::text as group_id,
  'STUDENT'::text as role,
  e.enrolled_at as valid_from,
  null::timestamp as valid_to,
  true as active
from enrollments e
join sections s on s.id = e.section_id
join courses c on c.id = s.course_id
where e.status = 'ENROLLED'

union all

select
  si.instructor_id::text as person_id,
  (c.course_code || '-' || s.section_no || '-' || s.term_code)::text as group_id,
  'INSTRUCTOR'::text as role,
  si.assigned_at as valid_from,
  null::timestamp as valid_to,
  true as active
from section_instructors si
join sections s on s.id = si.section_id
join courses c on c.id = s.course_id
where si.active = true;`}</code></pre>
                    </div>

                    <div class="rounded-md border p-3">
                        <p class="font-medium text-foreground mb-1">How to use these in UI</p>
                        <p>In <a class="underline" href="/audience">Audience</a>, choose <code>sourceType=JDBC</code>, set <code>connectionRef</code>, and run three ingestions by changing <code>entityType</code> and query.</p>
                        <p>After ingestion, simulate/publish rule that matches <code>STUDENT</code> to <code>INSTRUCTOR</code> within same <code>group_id</code>.</p>
                    </div>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium text-foreground">Scenario Blueprint 2: Peer review inside department/team</summary>
                <div class="mt-2 space-y-2">
                    <p><strong>PERSON:</strong> all employees.</p>
                    <p><strong>GROUP:</strong> optional team/department groups (recommended for strict boundary).</p>
                    <p><strong>MEMBERSHIP:</strong> person->team role <code>MEMBER</code>.</p>
                    <p><strong>Rule intent:</strong> <code>ATTRIBUTE_MATCH</code> by <code>department</code> or by group membership boundary.</p>
                    <p><strong>Outcome:</strong> peers are selected only from same department/team scope.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium text-foreground">Scenario Blueprint 3: Manager evaluates direct reports</summary>
                <div class="mt-2 space-y-2">
                    <p><strong>PERSON:</strong> all employees with manager reference attributes.</p>
                    <p><strong>GROUP:</strong> optional org-unit groups (not mandatory for pure hierarchy rule).</p>
                    <p><strong>MEMBERSHIP:</strong> optional, if you need additional scoping by org-unit.</p>
                    <p><strong>Rule intent:</strong> <code>MANAGER_HIERARCHY</code> using <code>managerId/supervisorId</code>.</p>
                    <p><strong>Outcome:</strong> each report is evaluated by manager according to hierarchy constraints.</p>
                </div>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium text-foreground">Scenario Blueprint 4: Committee/cross-functional evaluation pool</summary>
                <div class="mt-2 space-y-2">
                    <p><strong>PERSON:</strong> all committee members + evaluatees.</p>
                    <p><strong>GROUP:</strong> committee groups (e.g., accreditation committee).</p>
                    <p><strong>MEMBERSHIP:</strong> person->committee roles such as <code>REVIEWER</code>, <code>SUBJECT</code>.</p>
                    <p><strong>Rule intent:</strong> <code>ROUND_ROBIN</code> or <code>ALL_TO_ALL</code> with self-evaluation disabled.</p>
                    <p><strong>Outcome:</strong> balanced assignment distribution across reviewer pool.</p>
                </div>
            </details>

            <div class="rounded-md border p-3">
                <p class="font-medium text-foreground mb-1">Minimum required keys to keep consistent</p>
                <p><strong>PERSON rows:</strong> <code>person_id</code>, plus display/email attributes as needed.</p>
                <p><strong>GROUP rows:</strong> <code>group_id</code>, optional <code>group_type/group_name</code>.</p>
                <p><strong>MEMBERSHIP rows:</strong> <code>person_id</code>, <code>group_id</code>, <code>role</code>, optional validity window.</p>
                <p>
                    Keep IDs stable across semesters. New semester should create/update groups and memberships, not rewrite identity scheme.
                </p>
            </div>

            <div class="rounded-md border p-3">
                <p class="font-medium text-foreground mb-1">Configuration checklist (every scenario)</p>
                <p>1. In <a class="underline" href="/audience">Audience</a>, ingest PERSON -> GROUP -> MEMBERSHIP for current cycle.</p>
                <p>2. Validate mappings and inspect rejection logs before live replay.</p>
                <p>3. In <a class="underline" href="/admin/rules">Admin Rules</a>, simulate with diagnostics and verify pairing logic.</p>
                <p>4. In <a class="underline" href="/campaigns">Campaigns</a>, publish assignments to the target campaign.</p>
                <p>5. In <a class="underline" href="/reports">Reports</a>, verify counts and output quality.</p>
            </div>
        </Card.Content>
    </Card.Root>

    <Card.Root>
        <Card.Header>
            <Card.Title>Detailed FAQ</Card.Title>
        </Card.Header>
        <Card.Content class="space-y-4 text-sm">
            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Do evaluatees configure audience or rules?</summary>
                <p class="mt-2 text-muted-foreground">No. Only admins configure audience ingestion, mappings, rules, and campaign publishing.</p>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">Do we need to recreate mapping and rules every semester?</summary>
                <p class="mt-2 text-muted-foreground">No. Reuse mapping profiles and rule definitions; each semester you mainly ingest new data and create/publish a new campaign.</p>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">What is the minimum safe release process for a new semester?</summary>
                <p class="mt-2 text-muted-foreground">Ingest dry-run -> Ingest live -> Rule simulate dry-run -> Publish dry-run -> Publish live -> Verify reports.</p>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">When should we use dry run?</summary>
                <p class="mt-2 text-muted-foreground">Before every live publish or replay, especially after source or rule changes.</p>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">How do we avoid manual IDs?</summary>
                <p class="mt-2 text-muted-foreground">Use dropdown selectors in Audience/Admin Rules pages for profile, run, rule, request, and campaign where available.</p>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">How do we handle failed ingestion rows?</summary>
                <p class="mt-2 text-muted-foreground">Open run details, inspect rejections, fix source data/mapping, then replay dry-run and replay live.</p>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">How do we pick JDBC connectionRef?</summary>
                <p class="mt-2 text-muted-foreground">Use only keys configured in backend under audience JDBC connections. If no keys are configured, JDBC source cannot run.</p>
            </details>

            <details class="rounded-md border p-4">
                <summary class="cursor-pointer font-medium">What if admin sees JSON editors and wants form UI?</summary>
                <p class="mt-2 text-muted-foreground">Keep advanced JSON mode off. Use normal row/table builders on Audience and Admin Rules pages.</p>
            </details>
        </Card.Content>
    </Card.Root>
</div>
